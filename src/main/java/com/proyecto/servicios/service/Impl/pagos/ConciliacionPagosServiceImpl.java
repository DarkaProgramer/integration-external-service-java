package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.service.pagos.ConciliacionPagosService;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConciliacionPagosServiceImpl implements ConciliacionPagosService {

    private final PagoServicioRepository pagoRepository;
    private final PagoServicioTransacciones transacciones;
    private final ProveedorPagosGateway proveedorPagosGateway;
    private final PagosProperties properties;
    private final Clock clock;

    @Override
    public int conciliarPendientes() {
        PagosProperties.Conciliacion config = properties.getConciliacion();
        LocalDateTime pendientesAntesDe = LocalDateTime.now(clock).minusMinutes(config.getPendienteMinutos());
        List<Long> ids = pagoRepository.findIdsParaConciliar(pendientesAntesDe, PageRequest.of(0, config.getLote()));
        int resueltos = 0;
        for (Long id : ids) {
            try {
                if (conciliar(id)) {
                    resueltos++;
                }
            } catch (RuntimeException e) {
                // Un pago problemático no detiene al resto del lote
                log.error("Error al conciliar pago id={}: causa={}", id, e.getClass().getSimpleName());
            }
        }
        if (!ids.isEmpty()) {
            log.info("Conciliación: revisados={} resueltos={}", ids.size(), resueltos);
        }
        return resueltos;
    }

    private boolean conciliar(Long id) {
        PagoServicio pago = pagoRepository.findById(id).orElseThrow();
        Optional<ResultadoProveedor> resultado = proveedorPagosGateway.consultarEstado(SolicitudPagoProveedor.de(pago))
                .filter(r -> r.estado() != ResultadoProveedor.Estado.INDETERMINADA);
        if (resultado.isPresent()) {
            transacciones.aplicarResultado(id, resultado.get());
            return true;
        }
        // Sin respuesta concluyente: NO se reenvía ni se devuelve el dinero; se reintenta la consulta más tarde
        transacciones.registrarIntentoSinResultado(id);
        return false;
    }
}
