package com.proyecto.servicios.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Valida el token Bearer y, si pertenece a una sesión, que esa sesión siga abierta (logout inmediato).
 */
@Component
@RequiredArgsConstructor
public class ClienteActualArgumentResolver implements HandlerMethodArgumentResolver {

    private final TokenClienteService tokenClienteService;
    private final VerificadorSesion verificadorSesion;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ClienteActual.class)
                && ClienteAutenticado.class.equals(parameter.getParameterType());
    }

    @Override
    public ClienteAutenticado resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        ClienteAutenticado cliente = tokenClienteService.autenticar(webRequest.getHeader(HttpHeaders.AUTHORIZATION));
        if (cliente.sesionId() != null) {
            verificadorSesion.verificarActiva(cliente);
        }
        return cliente;
    }
}
