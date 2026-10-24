package com.proyecto.servicios.exception;

import com.proyecto.servicios.controller.GestoPagoProductController;
import com.proyecto.servicios.model.comun.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de integracion con GestoPago a la misma estructura {@link ErrorResponse}
 * que usa el resto del API. Tiene prioridad sobre el manejador global para este controller.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = GestoPagoProductController.class)
public class GestoPagoExceptionHandler {

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationException(GestoPagoIntegrationException ex, HttpServletRequest request) {
        GestoPagoErrorType type = ex.getErrorType();
        ErrorResponse response = ErrorResponse.of(type.getHttpStatus(), "Error de integración con GestoPago",
                type.getMensaje(), request, null);
        // Se conserva el código propio de la integración (101-105) para que el consumidor pueda identificarlo
        response.setCodigo(type.getCodigo());
        return ResponseEntity.status(type.getHttpStatus()).body(response);
    }
}
