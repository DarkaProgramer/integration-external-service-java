package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: el pago no trae una validación vigente que coincida con el producto y la referencia. */
public class ReferenciaNoValidadaException extends PagoException {

    public ReferenciaNoValidadaException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Referencia no validada", "Valida la referencia antes de pagar (la validación expira a los pocos minutos)");
    }
}
