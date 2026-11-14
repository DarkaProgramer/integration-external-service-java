package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: la referencia no cumple el formato del producto o el proveedor no la reconoce. */
public class ReferenciaInvalidaException extends PagoException {

    public ReferenciaInvalidaException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Referencia inválida", mensaje);
    }
}
