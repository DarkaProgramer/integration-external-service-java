package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.exception.pagos.EstadoPagoInvalidoException;
import com.proyecto.servicios.exception.pagos.IdempotencyKeyInvalidaException;
import com.proyecto.servicios.exception.pagos.IdempotencyKeyReutilizadaException;
import com.proyecto.servicios.exception.pagos.OperacionNoAutorizadaException;
import com.proyecto.servicios.exception.pagos.PagoNoEncontradoException;
import com.proyecto.servicios.model.comun.PaginaDto;
import com.proyecto.servicios.model.pagos.HistorialPagosRequestDto;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.PagoServicioResponseDto;
import com.proyecto.servicios.model.pagos.PatronesPago;
import com.proyecto.servicios.model.pagos.ResolucionManualRequestDto;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.PagoServicioService;
import com.proyecto.servicios.service.pagos.AutorizacionTarjeta;
import com.proyecto.servicios.service.pagos.TarjetasService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import com.proyecto.servicios.service.pagos.ValidacionReferenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Orquesta el pago de un servicio. Estrategia: <b>reservar (descontar) primero, enviar después, y compensar
 * solo con certeza</b>.
 * <ol>
 *     <li>Validaciones sin efectos: permiso del token, llave de idempotencia, producto y monto en el catálogo.</li>
 *     <li>Transacción corta: con la cuenta bloqueada se valida titular, estatus, duplicados, límite y saldo; se
 *     descuenta el saldo y se registra el pago PENDIENTE.</li>
 *     <li>Fuera de transacción: se envía a Gestopago, sin reintentos automáticos.</li>
 *     <li>Transacción corta: APROBADA, RECHAZADA (con devolución) o EN_CONCILIACION (dinero retenido).</li>
 * </ol>
 * Descontar antes de llamar al proveedor evita que el cliente gaste el mismo saldo dos veces mientras se espera
 * la respuesta; la alternativa (llamar primero y descontar después) puede dejar un pago aplicado sin fondos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicioServiceImpl implements PagoServicioService {

    private static final Pattern UUID = Pattern.compile(PatronesPago.UUID);
    private static final int TAMANO_HISTORIAL = 20;
    private static final Set<EstatusPago> RESOLUBLES_MANUALMENTE = EnumSet.of(EstatusPago.EN_CONCILIACION, EstatusPago.REVISION_MANUAL);

    private final CatalogoProductosService catalogoProductosService;
    private final ProveedorPagosGateway proveedorPagosGateway;
    private final PagoServicioTransacciones transacciones;
    private final PagoServicioRepository pagoRepository;
    private final PagoServicioMapper mapper;
    private final ValidacionReferenciaService validacionReferenciaService;
    private final TarjetasService tarjetasService;

    @Override
    public ResultadoPagoServicio pagar(ClienteAutenticado cliente, String idempotencyKey, PagoServicioRequestDto solicitud) {
        exigirPermiso(cliente, ClienteAutenticado.PERMISO_PAGAR_SERVICIOS);
        if (idempotencyKey == null || !UUID.matcher(idempotencyKey).matches()) {
            throw new IdempotencyKeyInvalidaException();
        }
        String llave = idempotencyKey.toLowerCase();
        String huella = huella(solicitud);

        // Reintento del cliente (p. ej. tras un timeout de su lado): se devuelve el resultado original sin cobrar otra vez
        var previo = pagoRepository.findByClienteIdAndIdempotencyKey(cliente.clienteId(), llave);
        if (previo.isPresent()) {
            return repetir(previo.get(), huella);
        }

        // Sin una validación vigente de la referencia con Gestopago no se cobra nada
        validacionReferenciaService.verificarParaPago(cliente, solicitud);

        ProductoCatalogo producto = catalogoProductosService.validarProductoYMonto(
                solicitud.getIdServicio(), solicitud.getIdProducto(), solicitud.getMonto());

        // Pago de contado con tarjeta de débito (guardada o nueva): el banco autoriza el cargo antes de registrar
        // el pago. Si el registro falla o resulta ser un reintento, el cargo se reembolsa.
        AutorizacionTarjeta tarjeta = tarjetasService.cobrar(cliente, solicitud);

        PagoServicioTransacciones.Reserva reserva;
        try {
            reserva = transacciones.reservarFondos(cliente, llave, huella, solicitud, producto, tarjeta);
        } catch (DataIntegrityViolationException e) {
            reembolsar(tarjeta, solicitud);
            // Dos peticiones simultáneas con la misma llave: la restricción única dejó pasar solo una
            PagoServicio ganador = pagoRepository.findByClienteIdAndIdempotencyKey(cliente.clienteId(), llave)
                    .orElseThrow(() -> e);
            return repetir(ganador, huella);
        } catch (RuntimeException e) {
            reembolsar(tarjeta, solicitud);
            throw e;
        }
        if (!reserva.nueva()) {
            reembolsar(tarjeta, solicitud);
            return repetir(reserva.pago(), huella);
        }

        PagoServicio pago = reserva.pago();
        log.info("Pago reservado: folio={} cliente={} servicio={} producto={} monto={}", pago.getFolio(),
                cliente.clienteId(), pago.getIdServicio(), pago.getIdProducto(), pago.getMonto());

        ResultadoProveedor resultado = proveedorPagosGateway.enviarPago(SolicitudPagoProveedor.de(pago));
        PagoServicio actualizado = transacciones.aplicarResultado(pago.getId(), resultado);

        log.info("Pago finalizado: folio={} estatus={}", actualizado.getFolio(), actualizado.getEstatus());
        return new ResultadoPagoServicio(mapper.toDto(actualizado), false);
    }

    @Override
    public PagoServicioResponseDto consultar(ClienteAutenticado cliente, String folio) {
        exigirPermiso(cliente, ClienteAutenticado.PERMISO_PAGAR_SERVICIOS);
        return pagoRepository.findByFolioAndClienteId(folio.toLowerCase(), cliente.clienteId())
                .map(mapper::toDto)
                .orElseThrow(PagoNoEncontradoException::new);
    }

    @Override
    public PaginaDto<PagoServicioResponseDto> historial(ClienteAutenticado cliente, HistorialPagosRequestDto filtros) {
        exigirPermiso(cliente, ClienteAutenticado.PERMISO_PAGAR_SERVICIOS);
        HistorialPagosRequestDto f = filtros == null ? new HistorialPagosRequestDto() : filtros;
        PageRequest pagina = PageRequest.of(f.getPagina() == null ? 0 : f.getPagina(),
                f.getTamano() == null ? TAMANO_HISTORIAL : f.getTamano(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<PagoServicio> pagos = f.getResultado() == null
                ? pagoRepository.findByClienteId(cliente.clienteId(), pagina)
                : pagoRepository.findByClienteIdAndEstatusIn(cliente.clienteId(), f.getResultado().estatus(), pagina);
        return PaginaDto.de(pagos, mapper::toDto);
    }

    @Override
    public PagoServicioResponseDto resolverManualmente(ClienteAutenticado operador, ResolucionManualRequestDto resolucion) {
        exigirPermiso(operador, ClienteAutenticado.PERMISO_CONCILIAR);
        PagoServicio pago = pagoRepository.findByFolio(resolucion.getFolio().toLowerCase())
                .orElseThrow(PagoNoEncontradoException::new);
        if (!RESOLUBLES_MANUALMENTE.contains(pago.getEstatus())) {
            throw new EstadoPagoInvalidoException();
        }
        ResultadoProveedor resultado = "APROBADA".equals(resolucion.getResultado())
                ? ResultadoProveedor.aprobada(resolucion.getFolioProveedor(), "MANUAL")
                : ResultadoProveedor.rechazada("MANUAL");
        PagoServicio actualizado = transacciones.aplicarResultado(pago.getId(), resultado);
        log.warn("Pago resuelto manualmente: folio={} estatus={} operador={}", actualizado.getFolio(),
                actualizado.getEstatus(), operador.clienteId());
        return mapper.toDto(actualizado);
    }

    private ResultadoPagoServicio repetir(PagoServicio pago, String huella) {
        if (!pago.getHuellaSolicitud().equals(huella)) {
            throw new IdempotencyKeyReutilizadaException();
        }
        log.info("Solicitud repetida con la misma llave: folio={} estatus={}", pago.getFolio(), pago.getEstatus());
        return new ResultadoPagoServicio(mapper.toDto(pago), true);
    }

    private void exigirPermiso(ClienteAutenticado cliente, String permiso) {
        if (!cliente.tienePermiso(permiso)) {
            throw new OperacionNoAutorizadaException();
        }
    }

    /** SHA-256 de los datos que definen la operación (el monto normalizado a 2 decimales: 100 == 100.00). */
    private void reembolsar(AutorizacionTarjeta tarjeta, PagoServicioRequestDto solicitud) {
        tarjetasService.reembolsar(tarjeta, solicitud.getMonto());
    }

    static String huella(PagoServicioRequestDto s) {
        String datos = String.join("|", s.getNumeroCuenta(), s.getIdServicio(), s.getIdProducto(), s.getReferencia(),
                s.getMonto().setScale(2, RoundingMode.HALF_UP).toPlainString());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(datos.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
