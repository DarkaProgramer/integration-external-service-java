package com.proyecto.servicios.exception.pagos;

import com.proyecto.servicios.model.comun.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de los errores del flujo de pagos con la misma estructura {@link ErrorResponse}
 * del resto del API. Los mensajes vienen de las excepciones (fijos y seguros); en el log solo se
 * registra el tipo de error y la ruta, nunca tokens, referencias ni saldos.
 * Va antes que {@code GlobalExceptionHandler} (que tiene el caso genérico Exception → 500).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice
public class PagoServicioExceptionHandler {

    @ExceptionHandler(PagoException.class)
    public ResponseEntity<ErrorResponse> handlePagoException(PagoException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatus();
        if (status.is5xxServerError()) {
            log.error("Pago no procesado: tipo={}, ruta={}", ex.getClass().getSimpleName(), request.getRequestURI());
        } else {
            log.warn("Solicitud de pago rechazada: tipo={}, status={}, ruta={}", ex.getClass().getSimpleName(), status.value(), request.getRequestURI());
        }
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(status);
        if (status == HttpStatus.UNAUTHORIZED) {
            respuesta.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        return respuesta.body(ErrorResponse.of(status, ex.getError(), ex.getMessage(), request, null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleEncabezadoFaltante(MissingRequestHeaderException ex, HttpServletRequest request) {
        if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(ex.getHeaderName())) {
            return handlePagoException(new AutenticacionClienteException(), request);
        }
        return ResponseEntity.badRequest().body(ErrorResponse.of(HttpStatus.BAD_REQUEST, "Encabezado faltante",
                "El encabezado " + ex.getHeaderName() + " es obligatorio", request, null));
    }
}
