package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 409: se intentó resolver manualmente un pago que ya es final. */
public class EstadoPagoInvalidoException extends PagoException {

    public EstadoPagoInvalidoException() {
        super(HttpStatus.CONFLICT, "Estatus no válido", "El pago ya tiene un resultado final y no puede modificarse");
    }
}
