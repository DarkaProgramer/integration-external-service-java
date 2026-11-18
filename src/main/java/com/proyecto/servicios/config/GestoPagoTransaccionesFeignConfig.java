package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

/**
 * Configuración exclusiva del cliente Feign de transacciones (sin @Configuration para no aplicarla a otros clientes).
 */
@Slf4j
public class GestoPagoTransaccionesFeignConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Token renovado automáticamente con GESTOPAGO_PASSWORD o, si no hay, el de la variable de entorno.
     * Si no hay ninguno, la petición no sale (y el pago se rechaza sin cargo).
     */
    @Bean
    public RequestInterceptor gestoPagoTransaccionesAuthInterceptor(
            @Value("${gestopago.tx.bearer-token:}") String bearerToken,
            ObjectProvider<GestoPagoBearerToken> tokenDinamico) {
        return template -> {
            GestoPagoBearerToken proveedor = tokenDinamico.getIfAvailable();
            String token = proveedor != null ? proveedor.obtener(bearerToken) : bearerToken;
            if (StringUtils.isBlank(token)) {
                log.error("No se ha configurado gestopago.tx.bearer-token ni GESTOPAGO_PASSWORD");
                throw new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION);
            }
            template.header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token);
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
