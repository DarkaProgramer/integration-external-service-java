package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 403: el token no tiene el permiso necesario, la cuenta no es del cliente o no existe (no se distingue para no revelar cuentas ajenas). */
public class OperacionNoAutorizadaException extends PagoException {

    public OperacionNoAutorizadaException() {
        super(HttpStatus.FORBIDDEN, "Operación no autorizada", "No está autorizado para realizar esta operación con la cuenta indicada");
    }
}
