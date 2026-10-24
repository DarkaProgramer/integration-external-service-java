package com.proyecto.servicios.model.comun;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Estructura uniforme para todas las respuestas exitosas del API.
 * Hereda {@code codigo} y {@code mensaje} de {@link GenericResponse} (codigo = 0).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"codigo", "mensaje", "timestamp", "status", "datos"})
public class ApiResponse<T> extends GenericResponse {

    private LocalDateTime timestamp;
    private int status;
    private T datos;

    public static <T> ApiResponse<T> of(int status, String mensaje, T datos) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(mensaje);
        response.setTimestamp(LocalDateTime.now());
        response.setStatus(status);
        response.setDatos(datos);
        return response;
    }
}
