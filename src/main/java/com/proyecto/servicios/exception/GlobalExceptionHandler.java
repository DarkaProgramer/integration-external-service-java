package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.comun.ErrorResponse;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Manejador global: centraliza la traducción de excepciones a respuestas HTTP con
 * la estructura {@link ErrorResponse}. Los controladores no usan try-catch.
 * Los errores de validación de los DTO los atiende {@link ValidadorCampos}, que tiene
 * mayor prioridad; este manejador se evalúa después (incluye el caso genérico Exception).
 */
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ───────────── Errores de negocio ─────────────

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteYaRegistrado(ClienteYaRegistradoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Cliente duplicado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(CorreoYaRegistradoException.class)
    public ResponseEntity<ErrorResponse> handleCorreoYaRegistrado(CorreoYaRegistradoException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Correo duplicado", ex.getMessage(), request, null);
    }

    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class, PersonaNoEncontradaException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage(), request, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> handleReglaNegocio(ReglaNegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Regla de negocio no cumplida", ex.getMessage(), request, null);
    }

    // ───────────── Errores de entrada ─────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMensajeIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        if (ex.getCause() instanceof UnrecognizedPropertyException campoDesconocido) {
            return build(HttpStatus.BAD_REQUEST, "Campo no permitido",
                    "El campo '" + campoDesconocido.getPropertyName() + "' no existe o no puede ser modificado por el cliente",
                    request, null);
        }
        return build(HttpStatus.BAD_REQUEST, "Solicitud mal formada",
                "El cuerpo de la solicitud no es un JSON válido o algún campo tiene un tipo de dato incorrecto (las fechas usan el formato yyyy-MM-dd)",
                request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método " + ex.getMethod() + " no está soportado para esta ruta", request, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNoSoportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de contenido no soportado",
                "El contenido debe enviarse como application/json", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRutaNoEncontrada(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Ruta no encontrada",
                "La ruta solicitada no existe en el API", request, null);
    }

    // ───────────── Errores de persistencia e inesperados ─────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegridadDatos(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violación de integridad de datos en {}", request.getRequestURI(), ex);
        return build(HttpStatus.CONFLICT, "Conflicto de datos",
                "La operación entra en conflicto con información ya registrada (CURP, RFC, correo o número de cuenta duplicado)",
                request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErrorInesperado(Exception ex, HttpServletRequest request) {
        // El detalle técnico sólo se registra en el log del servidor, nunca se envía al cliente.
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor",
                "No fue posible completar la operación por un problema interno. Intente nuevamente más tarde o contacte al administrador",
                request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String mensaje,
                                                HttpServletRequest request, Map<String, String> detalles) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, error, mensaje, request, detalles));
    }
}
