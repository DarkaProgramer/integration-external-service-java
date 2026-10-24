package com.proyecto.servicios.model.comun;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.proyecto.servicios.model.GenericResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura uniforme para todas las respuestas de error del API.
 * Hereda {@code codigo} y {@code mensaje} de {@link GenericResponse} (codigo = 1).
 * Nunca contiene trazas, consultas SQL ni detalles internos.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"codigo", "mensaje", "timestamp", "status", "error", "ruta", "detalles"})
public class ErrorResponse extends GenericResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String ruta;
    private Map<String, String> detalles;

    public static ErrorResponse of(HttpStatus status, String error, String mensaje,
                                   HttpServletRequest request, Map<String, String> detalles) {
        ErrorResponse response = new ErrorResponse();
        response.setCodigo(CODIGO_ERROR);
        response.setMensaje(mensaje);
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status.value());
        response.setError(error);
        response.setRuta(request != null ? request.getRequestURI() : null);
        response.setDetalles(detalles);
        return response;
    }
}
