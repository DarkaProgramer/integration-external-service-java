package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Configuración exclusiva del cliente Feign de transacciones (sin @Configuration para no aplicarla a otros clientes).
 */
@Slf4j
public class GestoPagoTransaccionesFeignConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    /** El token llega por variable de entorno; si falta, la petición no sale (y el pago se rechaza sin cargo). */
    @Bean
    public RequestInterceptor gestoPagoTransaccionesAuthInterceptor(
            @Value("${gestopago.tx.bearer-token:}") String bearerToken) {
        return template -> {
            if (StringUtils.isBlank(bearerToken)) {
                log.error("No se ha configurado gestopago.tx.bearer-token");
                throw new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION);
            }
            template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + bearerToken);
        };
    }

    /**
     * Nunca reintentar automáticamente una operación financiera: si la primera petición sí llegó al proveedor,
     * un reintento podría cobrar dos veces. Los casos dudosos se resuelven por conciliación.
     */
    @Bean
    public Retryer gestoPagoTransaccionesRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /** Sin log de Feign: en niveles HEADERS/FULL imprimiría el token y la referencia del cliente. */
    @Bean
    public Logger.Level gestoPagoTransaccionesLoggerLevel() {
        return Logger.Level.NONE;
    }
}
