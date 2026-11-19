package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 402: el banco emisor declinó el cargo. No se hizo ningún cargo. */
public class TarjetaRechazadaException extends PagoException {

    public TarjetaRechazadaException(String mensaje) {
        super(HttpStatus.PAYMENT_REQUIRED, "Pago declinado", mensaje);
    }
}
