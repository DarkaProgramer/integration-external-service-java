package com.proyecto.servicios.exception;

/**
 * Se lanza cuando los datos son válidos en formato pero no cumplen una regla del sistema
 * (por ejemplo: cliente menor de edad o cliente que ya fue dado de baja).
 */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String message) {
        super(message);
    }
}
