package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: no se informa el saldo en el mensaje. */
public class SaldoInsuficienteException extends PagoException {

    public SaldoInsuficienteException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente", "El saldo disponible no es suficiente para realizar el pago. Solo se aceptan pagos de contado: no hay crédito ni pagos parciales");
    }
}
