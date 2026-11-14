package com.proyecto.servicios.exception.auth;

import com.proyecto.servicios.exception.pagos.PagoException;
import org.springframework.http.HttpStatus;

/** 401: refresh token desconocido, ya usado, expirado o de una sesión cerrada. */
public class SesionInvalidaException extends PagoException {

    public SesionInvalidaException() {
        super(HttpStatus.UNAUTHORIZED, "Sesión inválida", "La sesión expiró o fue cerrada. Inicia sesión de nuevo");
    }
}
