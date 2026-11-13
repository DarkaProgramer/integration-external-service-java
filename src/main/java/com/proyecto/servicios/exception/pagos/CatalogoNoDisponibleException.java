package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 503: no se pudo consultar el catálogo; sin validarlo no se procesa nada (falla cerrada). */
public class CatalogoNoDisponibleException extends PagoException {

    public CatalogoNoDisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible", "No fue posible validar el producto en este momento. No se realizó ningún cargo; intente más tarde");
    }
}
