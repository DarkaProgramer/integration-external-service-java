package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: el pago no indica con qué tarjeta de débito se cobra. */
public class MedioPagoRequeridoException extends PagoException {

    public MedioPagoRequeridoException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Tarjeta requerida",
                "Selecciona una tarjeta guardada (con su CVV) o agrega una tarjeta de débito para pagar");
    }
}
