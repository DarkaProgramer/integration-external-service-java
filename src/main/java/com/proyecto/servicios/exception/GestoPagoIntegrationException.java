package com.proyecto.servicios.exception;

import lombok.Getter;

@Getter
public class GestoPagoIntegrationException extends RuntimeException {

    private final GestoPagoErrorType errorType;

    public GestoPagoIntegrationException(GestoPagoErrorType errorType) {
        super(errorType.getMensaje());
        this.errorType = errorType;
    }

    public GestoPagoIntegrationException(GestoPagoErrorType errorType, Throwable cause) {
        super(errorType.getMensaje(), cause);
        this.errorType = errorType;
    }
}
