package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTransaccionesFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Cliente de bajo nivel de sendTx.do. Devuelve el cuerpo crudo: la interpretación (XML o JSON, códigos)
 * la hace {@code GestoPagoPagosGateway}, para que un cambio de formato no afecte al resto de capas.
 * SUPUESTO: parámetros como formulario (application/x-www-form-urlencoded), igual que el endpoint de
 * autenticación de Gestopago. Ajustar aquí cuando se tenga el contrato oficial.
 */
@FeignClient(
        name = "gestoPagoTransacciones",
        url = "${gestopago.tx.url}",
        configuration = GestoPagoTransaccionesFeignConfig.class)
public interface GestoPagoTransaccionesClient {

    @PostMapping(value = "/sistema/service/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(@RequestBody Map<String, ?> parametros);
}
