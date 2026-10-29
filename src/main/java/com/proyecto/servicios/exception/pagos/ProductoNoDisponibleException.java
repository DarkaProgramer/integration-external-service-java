package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 422: la combinación servicio/producto no está en el catálogo del proveedor. */
public class ProductoNoDisponibleException extends PagoException {

    public ProductoNoDisponibleException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Producto no válido", "El servicio o producto indicado no existe en el catálogo");
    }
}
