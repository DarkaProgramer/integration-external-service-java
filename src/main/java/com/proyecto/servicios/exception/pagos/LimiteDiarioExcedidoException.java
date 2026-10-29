package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: control contra operaciones indebidas (p. ej. uso de un token robado). */
public class LimiteDiarioExcedidoException extends PagoException {

    public LimiteDiarioExcedidoException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Límite diario excedido", "El pago excede el límite diario permitido para la cuenta");
    }
}
