package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 404: el folio no existe o no pertenece al cliente. */
public class PagoNoEncontradoException extends PagoException {

    public PagoNoEncontradoException() {
        super(HttpStatus.NOT_FOUND, "Pago no encontrado", "No existe un pago con el folio indicado");
    }
}
