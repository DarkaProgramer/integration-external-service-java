package com.proyecto.servicios.config;

import com.proyecto.servicios.seguridad.ClienteActualArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SeguridadWebConfig implements WebMvcConfigurer {

    private final ClienteActualArgumentResolver clienteActualArgumentResolver;
    private final String[] origenesPermitidos;

    public SeguridadWebConfig(ClienteActualArgumentResolver clienteActualArgumentResolver,
                              @Value("${app.cors.origenes:}") String origenes) {
        this.clienteActualArgumentResolver = clienteActualArgumentResolver;
        this.origenesPermitidos = Arrays.stream(origenes.split(",")).map(String::strip).filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    /**
     * CORS solo para los orígenes configurados del frontend web (nunca "*"). El token viaja en el encabezado
     * Authorization, no en cookies, así que no se habilitan credenciales.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origenesPermitidos.length == 0) {
            return;
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key")
                .exposedHeaders("Idempotent-Replayed", "WWW-Authenticate")
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clienteActualArgumentResolver);
    }
}
