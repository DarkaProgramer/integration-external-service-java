package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: el monto no coincide con el precio del producto o está fuera del rango permitido. */
public class MontoNoPermitidoException extends PagoException {

    public MontoNoPermitidoException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Monto no permitido", mensaje);
    }
}
