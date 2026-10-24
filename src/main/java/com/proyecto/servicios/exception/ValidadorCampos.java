package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.comun.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validador de campos (basado en el ejemplo visto en clase).
 * Centraliza los errores de Bean Validation de todos los DTO: los controladores sólo usan @Valid
 * y no repiten validaciones ni bloques try-catch.
 * Tiene la mayor prioridad para que estos errores no los atienda el caso genérico de
 * {@link GlobalExceptionHandler}.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ValidadorCampos {

    static final String MENSAJE_DATOS_NO_VALIDOS = "Datos no válidos: revise el detalle de cada campo";

    /**
     * En caso de que alguna validación no se cumpla para algún DTO se lanza MethodArgumentNotValidException;
     * este método la atrapa y regresa al cliente el mensaje (message) de cada campo inválido.
     *
     * @param errors errores de validación del DTO (MethodArgumentNotValidException implementa Errors)
     * @return 400 BAD REQUEST con codigo = 1, mensaje y el detalle campo → mensaje
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> dtoValidation(Errors errors, HttpServletRequest request) {
        Map<String, String> listError = new LinkedHashMap<>();
        for (FieldError fieldError : errors.getFieldErrors()) {
            listError.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        // Errores a nivel objeto (validaciones cruzadas con @AssertTrue sobre métodos que no son propiedades)
        for (ObjectError globalError : errors.getGlobalErrors()) {
            listError.putIfAbsent(globalError.getObjectName(), globalError.getDefaultMessage());
        }
        return respuesta(listError, request);
    }

    /**
     * Validaciones declaradas directamente sobre parámetros (por ejemplo con @Validated en un servicio).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> parametrosValidation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> listError = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(v -> listError.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return respuesta(listError, request);
    }

    private ResponseEntity<GenericResponse> respuesta(Map<String, String> listError, HttpServletRequest request) {
        ErrorResponse genericResponse = ErrorResponse.of(HttpStatus.BAD_REQUEST, "Error de validación",
                MENSAJE_DATOS_NO_VALIDOS, request, listError);
        return new ResponseEntity<>(genericResponse, HttpStatus.BAD_REQUEST);
    }
}
