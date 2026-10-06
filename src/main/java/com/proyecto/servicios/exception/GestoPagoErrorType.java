package com.proyecto.servicios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Tipos de error de la integracion con GestoPago.
 * El mensaje es seguro para exponerse al consumidor (no contiene datos sensibles).
 */
@Getter
public enum GestoPagoErrorType {

    COMMUNICATION(101, HttpStatus.BAD_GATEWAY, "No fue posible comunicarse con el servicio externo"),
    TIMEOUT(102, HttpStatus.GATEWAY_TIMEOUT, "El servicio externo no respondio en el tiempo esperado"),
    AUTHENTICATION(103, HttpStatus.BAD_GATEWAY, "Error de autenticacion con el servicio externo"),
    UNSUCCESSFUL_RESPONSE(104, HttpStatus.BAD_GATEWAY, "El servicio externo respondio con un error"),
    INVALID_RESPONSE(105, HttpStatus.BAD_GATEWAY, "El servicio externo devolvio una respuesta invalida");

    private final int codigo;
    private final HttpStatus httpStatus;
    private final String mensaje;

    GestoPagoErrorType(int codigo, HttpStatus httpStatus, String mensaje) {
        this.codigo = codigo;
        this.httpStatus = httpStatus;
        this.mensaje = mensaje;
    }
}
