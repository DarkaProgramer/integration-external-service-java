package com.proyecto.servicios.exception.auth;

import com.proyecto.servicios.exception.pagos.PagoException;
import org.springframework.http.HttpStatus;

/** 429: se superó el número de intentos fallidos de login. */
public class AccesoBloqueadoException extends PagoException {

    public AccesoBloqueadoException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Acceso bloqueado temporalmente", "Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
    }
}
