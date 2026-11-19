package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Login del distribuidor en Gestopago. Los datos (incluida la contraseña) viajan en el CUERPO del POST como
 * formulario, no en la URL: una URL queda en logs de proxies y servidores.
 * Tiempos límite: {@code spring.cloud.openfeign.client.config.gestoPagoAuth.*} en application.properties.
 */
@FeignClient(name = "gestoPagoAuth", url = "${gestopago.auth.url}")
public interface GestoPagoAuthClient {

    @PostMapping(value = "/sistema/app/jwt-gp/authenticate/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    GestoPagoAuthResponse authenticate(@RequestBody Map<String, ?> credenciales);
}
