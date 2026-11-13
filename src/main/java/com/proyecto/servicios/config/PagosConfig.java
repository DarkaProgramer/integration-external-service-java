package com.proyecto.servicios.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(PagosProperties.class)
public class PagosConfig {

    /** Reloj inyectable: permite probar ventanas de tiempo (duplicados, límite diario, expiración de tokens). */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
