package com.proyecto.servicios.exception.auth;

import com.proyecto.servicios.exception.pagos.PagoException;
import org.springframework.http.HttpStatus;

/** 404: la sesión no existe o es de otro usuario. */
public class SesionNoEncontradaException extends PagoException {

    public SesionNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "Sesión no encontrada", "La sesión no existe");
    }
}
