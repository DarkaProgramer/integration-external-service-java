package com.proyecto.servicios.exception;

import com.proyecto.servicios.controller.GestoPagoProductController;
import com.proyecto.servicios.model.GenericResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de integracion a la respuesta estandar {@link GenericResponse}.
 * Se limita al controller de GestoPago para no alterar el comportamiento de los demas endpoints.
 */
@RestControllerAdvice(assignableTypes = GestoPagoProductController.class)
public class GestoPagoExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleIntegrationException(GestoPagoIntegrationException ex) {
        GestoPagoErrorType type = ex.getErrorType();
        GenericResponse response = new GenericResponse();
        response.setCodigo(type.getCodigo());
        response.setMensaje(type.getMensaje());
        return ResponseEntity.status(type.getHttpStatus()).body(response);
    }
}
