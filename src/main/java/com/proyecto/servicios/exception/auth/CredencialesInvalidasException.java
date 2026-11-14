package com.proyecto.servicios.exception.auth;

import com.proyecto.servicios.exception.pagos.PagoException;
import org.springframework.http.HttpStatus;

/** 401: correo inexistente o contraseña incorrecta (mismo mensaje para no revelar qué correos existen). */
public class CredencialesInvalidasException extends PagoException {

    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "Correo o contraseña incorrectos");
    }
}
