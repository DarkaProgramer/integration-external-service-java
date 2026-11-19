package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Proveedor de pagos simulado: aprueba todos los pagos, salvo cuando la referencia termina en {@code 9999},
 * que se rechaza (y el dinero se devuelve) para poder probar ese caso. No mueve dinero real.
 */
@Primary
@Component
@ConditionalOnProperty(name = "gestopago.modo", havingValue = "simulado", matchIfMissing = true)
public class PagosSimulados implements ProveedorPagosGateway {

    @Override
    public ResultadoProveedor enviarPago(SolicitudPagoProveedor solicitud) {
        if (solicitud.referencia() != null && solicitud.referencia().endsWith("9999")) {
            return ResultadoProveedor.rechazada("SIM_RECHAZO");
        }
        return ResultadoProveedor.aprobada("SIM-" + solicitud.folio(), "SIM_00");
    }

    @Override
    public Optional<ResultadoProveedor> consultarEstado(SolicitudPagoProveedor solicitud) {
        return Optional.of(enviarPago(solicitud));
    }
}
