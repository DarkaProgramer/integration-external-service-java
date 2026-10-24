package com.proyecto.servicios.exception;

public class CorreoYaRegistradoException extends RuntimeException {
    public CorreoYaRegistradoException(String message) {
        super(message);
    }
}
