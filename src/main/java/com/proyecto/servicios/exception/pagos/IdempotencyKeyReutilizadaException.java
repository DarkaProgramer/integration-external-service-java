package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: la misma llave de idempotencia con otro contenido. */
public class IdempotencyKeyReutilizadaException extends PagoException {

    public IdempotencyKeyReutilizadaException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Llave de idempotencia reutilizada", "La llave Idempotency-Key ya se usó con datos diferentes; genere una nueva para otra operación");
    }
}
