package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoTransaccionesFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Consulta de referencia en Gestopago. La ruta se configura con {@code gestopago.referencia.path}
 * (variable GESTOPAGO_REFERENCIA_PATH) porque depende del contrato que entregue el proveedor.
 * Usa el mismo Bearer y la misma política sin reintentos que las transacciones.
 */
@FeignClient(
        name = "gestoPagoReferencias",
        url = "${gestopago.referencia.url}",
        configuration = GestoPagoTransaccionesFeignConfig.class)
public interface GestoPagoReferenciasClient {

    @PostMapping(value = "${gestopago.referencia.path:/}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String consultar(@RequestBody Map<String, ?> parametros);
}
