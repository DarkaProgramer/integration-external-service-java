package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Validación de referencias simulada. Toda referencia con formato correcto es válida, salvo las que
 * empiezan con {@code 0000}, que se responden como inexistentes para poder probar ese caso.
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
        return ResultadoValidacionReferencia.valida(null, "SIM_00");
    }
}
