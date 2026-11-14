package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 503: no se pudo consultar la referencia a Gestopago (falla cerrada: sin validación no se paga). */
public class ValidacionNoDisponibleException extends PagoException {

    public ValidacionNoDisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Validación no disponible", "No fue posible validar la referencia con el proveedor en este momento. Intenta más tarde");
    }
}
