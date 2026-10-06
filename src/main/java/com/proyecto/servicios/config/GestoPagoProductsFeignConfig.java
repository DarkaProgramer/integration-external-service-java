package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Configuracion del cliente Feign de productos de GestoPago.
 * No se anota con @Configuration para que NO se aplique globalmente a otros clientes Feign.
 */
@Slf4j
public class GestoPagoProductsFeignConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public RequestInterceptor gestoPagoProductsAuthInterceptor(
            @Value("${gestopago.products.bearer-token:}") String bearerToken) {
        return template -> {
            if (StringUtils.isBlank(bearerToken)) {
                log.error("No se ha configurado gestopago.products.bearer-token");
                throw new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION);
            }
            template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + bearerToken);
        };
    }
}
