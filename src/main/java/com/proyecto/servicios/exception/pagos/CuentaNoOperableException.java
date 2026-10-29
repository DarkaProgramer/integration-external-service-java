package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: la cuenta existe y es del cliente, pero no está ACTIVA. */
public class CuentaNoOperableException extends PagoException {

    public CuentaNoOperableException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Cuenta no operable", "La cuenta no se encuentra activa");
    }
}
