package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 400: el encabezado Idempotency-Key no es un UUID. */
public class IdempotencyKeyInvalidaException extends PagoException {

    public IdempotencyKeyInvalidaException() {
        super(HttpStatus.BAD_REQUEST, "Llave de idempotencia inválida", "El encabezado Idempotency-Key debe ser un UUID");
    }
}
