package com.proyecto.servicios.exception.pagos;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base de los errores del flujo de pagos. Cada subclase define su código HTTP y un mensaje
 * seguro para el consumidor (sin saldos, datos de otras cuentas ni detalles del proveedor),
 * así el manejador centralizado no necesita un método por excepción (abierto a extensión).
 */
@Getter
public abstract class PagoException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    protected PagoException(HttpStatus status, String error, String mensaje) {
        super(mensaje);
        this.status = status;
        this.error = error;
    }
}
