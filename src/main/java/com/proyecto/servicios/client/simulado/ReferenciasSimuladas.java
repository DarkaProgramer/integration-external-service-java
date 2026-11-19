package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validación de referencias simulada. Toda referencia con formato correcto es válida, salvo las que
 * empiezan con {@code 0000}, que se responden como inexistentes para poder probar ese caso.
 * Para los recibos (monto variable) informa un adeudo fijo por referencia, que se debe pagar completo (contado).
 */
@Primary
@Component
@ConditionalOnProperty(name = "gestopago.modo", havingValue = "simulado", matchIfMissing = true)
public class ReferenciasSimuladas implements ValidadorReferenciaGateway {

    @Override
    public ResultadoValidacionReferencia validar(ProductoCatalogo producto, String referencia) {
        if (referencia != null && referencia.startsWith("0000")) {
            return ResultadoValidacionReferencia.invalida("SIM_NO_EXISTE");
        }
        BigDecimal adeudo = producto.tienePrecioFijo() ? null : adeudo(referencia);
        return ResultadoValidacionReferencia.valida(adeudo, "SIM_00");
    }

    /** Adeudo de práctica entre $150.00 y $2,499.99, siempre el mismo para la misma referencia. */
    static BigDecimal adeudo(String referencia) {
        long centavos = 15_000 + Math.floorMod((long) referencia.hashCode() * 31 + 7, 235_000L);
        return BigDecimal.valueOf(centavos).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY);
    }
}
