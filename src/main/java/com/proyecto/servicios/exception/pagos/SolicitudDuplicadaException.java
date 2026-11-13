package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 409: mismo pago (cuenta, producto, referencia y monto) enviado con otra llave en la ventana anti duplicados. */
public class SolicitudDuplicadaException extends PagoException {

    public SolicitudDuplicadaException() {
        super(HttpStatus.CONFLICT, "Solicitud duplicada", "Ya existe un pago reciente con los mismos datos; verifique su estatus antes de intentarlo de nuevo");
    }
}
