package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.ValidacionReferencia;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.ReferenciaInvalidaException;
import com.proyecto.servicios.exception.pagos.ReferenciaNoValidadaException;
import com.proyecto.servicios.exception.pagos.ValidacionNoDisponibleException;
import com.proyecto.servicios.exception.pagos.ValidacionYaUsadaException;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.ValidacionReferenciaResponseDto;
import com.proyecto.servicios.model.pagos.ValidarReferenciaRequestDto;
import com.proyecto.servicios.repositorys.pagos.ValidacionReferenciaRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.ValidacionReferenciaService;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Flujo: 1) formato según el producto del catálogo, 2) consulta a Gestopago, 3) comprobante de corta duración.
 * Sin comprobante válido no se puede pagar (ver {@link #verificarParaPago}).
 */
@Slf4j
@Service
public class ValidacionReferenciaServiceImpl implements ValidacionReferenciaService {

    static final String MENSAJE_RECHAZO_PROVEEDOR = "El proveedor no reconoce la referencia. Verifica el número e intenta de nuevo";

    private final CatalogoProductosService catalogoProductosService;
    private final ValidadorReferenciaGateway gateway;
    private final ValidacionReferenciaRepository repository;
    private final Clock clock;
    private final Duration vigencia;

    public ValidacionReferenciaServiceImpl(CatalogoProductosService catalogoProductosService,
                                           ValidadorReferenciaGateway gateway,
                                           ValidacionReferenciaRepository repository,
                                           Clock clock,
                                           @Value("${pagos.validacion.vigencia-minutos:10}") long vigenciaMinutos) {
        this.catalogoProductosService = catalogoProductosService;
        this.gateway = gateway;
        this.repository = repository;
        this.clock = clock;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
    }

    @Override
    @Transactional
    public ValidacionReferenciaResponseDto validar(ClienteAutenticado cliente, ValidarReferenciaRequestDto solicitud) {
        exigirPermiso(cliente);
        String referencia = solicitud.getReferencia().strip();
        ProductoCatalogo producto = catalogoProductosService.obtener(solicitud.getIdServicio(), solicitud.getIdProducto());
        producto.referencia().errores(referencia).ifPresent(error -> {
            throw new ReferenciaInvalidaException(error);
        });

        ResultadoValidacionReferencia resultado = gateway.validar(producto, referencia);
        switch (resultado.estado()) {
            case INVALIDA -> throw new ReferenciaInvalidaException(MENSAJE_RECHAZO_PROVEEDOR);
            case NO_DISPONIBLE -> throw new ValidacionNoDisponibleException();
            case VALIDA -> log.debug("Referencia validada por el proveedor");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        ValidacionReferencia validacion = new ValidacionReferencia();
        validacion.setId(UUID.randomUUID().toString());
        validacion.setClienteId(cliente.clienteId());
        validacion.setIdServicio(producto.idServicio());
        validacion.setIdProducto(producto.idProducto());
        validacion.setReferencia(referencia);
        validacion.setMontoAdeudo(resultado.montoAdeudo());
        validacion.setCodigoProveedor(resultado.codigo());
        validacion.setCreatedAt(ahora);
        validacion.setExpiraEn(ahora.plus(vigencia));
        repository.save(validacion);

        return ValidacionReferenciaResponseDto.builder()
                .validacionId(validacion.getId())
                .idServicio(producto.idServicio())
                .idProducto(producto.idProducto())
                .producto(producto.nombre())
                .servicio(producto.servicio())
                .referencia(PagoServicioMapper.enmascarar(referencia))
                .precio(producto.tienePrecioFijo() ? producto.precio() : null)
                .montoAdeudo(resultado.montoAdeudo())
                .expiraEn(validacion.getExpiraEn())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public void verificarParaPago(ClienteAutenticado cliente, PagoServicioRequestDto solicitud) {
        String id = solicitud.getValidacionId() == null ? "" : solicitud.getValidacionId().toLowerCase(Locale.ROOT);
        ValidacionReferencia validacion = repository.findByIdAndClienteId(id, cliente.clienteId())
                .orElseThrow(ReferenciaNoValidadaException::new);
        boolean coincide = validacion.getIdServicio().equals(solicitud.getIdServicio())
                && validacion.getIdProducto().equals(solicitud.getIdProducto())
                && validacion.getReferencia().equals(solicitud.getReferencia());
        if (!coincide) {
            throw new ReferenciaNoValidadaException();
        }
        if (validacion.getUsadaEn() != null) {
            throw new ValidacionYaUsadaException();
        }
        if (!validacion.getExpiraEn().isAfter(LocalDateTime.now(clock))) {
            throw new ReferenciaNoValidadaException();
        }
    }

    private static void exigirPermiso(ClienteAutenticado cliente) {
        if (!cliente.tienePermiso(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS)) {
            throw new OperacionNoAutorizadaException();
        }
    }
}
