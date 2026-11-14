package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 409: el comprobante de validación ya se consumió en otro pago (evita pagar dos veces con la misma validación). */
public class ValidacionYaUsadaException extends PagoException {

    public ValidacionYaUsadaException() {
        super(HttpStatus.CONFLICT, "Validación ya utilizada", "Esta validación ya se usó en un pago. Revisa tu historial o valida la referencia de nuevo");
    }
}
