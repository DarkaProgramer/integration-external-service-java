package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: datos de tarjeta incorrectos o tarjeta no permitida (solo débito). No se hizo ningún cargo. */
public class TarjetaInvalidaException extends PagoException {

    public TarjetaInvalidaException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Tarjeta inválida", mensaje);
    }
}
