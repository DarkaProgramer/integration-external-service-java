package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 404: la tarjeta guardada no existe o no es del cliente (se responde igual para no revelar cuál). */
public class TarjetaNoEncontradaException extends PagoException {

    public TarjetaNoEncontradaException() {
        super(HttpStatus.NOT_FOUND, "Tarjeta no encontrada", "La tarjeta seleccionada ya no está disponible. Elige otra o agrega una nueva");
    }
}
