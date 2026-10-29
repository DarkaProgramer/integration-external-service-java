package com.proyecto.servicios.config;

import com.proyecto.servicios.seguridad.ClienteActualArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SeguridadWebConfig implements WebMvcConfigurer {

    private final ClienteActualArgumentResolver clienteActualArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clienteActualArgumentResolver);
    }
}
