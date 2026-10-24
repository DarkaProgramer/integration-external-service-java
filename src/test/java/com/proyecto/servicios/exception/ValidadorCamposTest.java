package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.comun.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del ValidadorCampos (manejo centralizado de errores de validación).
 */
@DisplayName("ValidadorCampos - Pruebas Unitarias")
class ValidadorCamposTest {

    private ValidadorCampos validador;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        validador = new ValidadorCampos();
        request = new MockHttpServletRequest("POST", "/api/clientes");
    }

    private MethodArgumentNotValidException excepcion(FieldError... errores) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "clienteRegistroRequestDto");
        for (FieldError error : errores) {
            bindingResult.addError(error);
        }
        return new MethodArgumentNotValidException(null, bindingResult);
    }

    @Test
    @DisplayName("Retorna 400 con codigo 1 y el mensaje de cada campo")
    void dtoValidation_retorna400ConCodigoYDetalle() {
        MethodArgumentNotValidException ex = excepcion(
                new FieldError("dto", "nombre", "El nombre es obligatorio"));

        ResponseEntity<GenericResponse> resp = validador.dtoValidation(ex, request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().getCodigo()).isEqualTo(GenericResponse.CODIGO_ERROR);
        assertThat(resp.getBody().getMensaje()).startsWith("Datos no válidos");
        assertThat(resp.getBody()).isInstanceOf(ErrorResponse.class);

        ErrorResponse body = (ErrorResponse) resp.getBody();
        assertThat(body.getStatus()).isEqualTo(400);
        assertThat(body.getRuta()).isEqualTo("/api/clientes");
        assertThat(body.getDetalles()).containsEntry("nombre", "El nombre es obligatorio");
    }

    @Test
    @DisplayName("Incluye todos los campos inválidos")
    void dtoValidation_multiplesErrores() {
        MethodArgumentNotValidException ex = excepcion(
                new FieldError("dto", "nombre", "Nombre obligatorio"),
                new FieldError("dto", "correo", "Correo inválido"),
                new FieldError("dto", "curp", "CURP inválida"));

        ErrorResponse body = (ErrorResponse) validador.dtoValidation(ex, request).getBody();

        assertThat(body.getDetalles()).hasSize(3).containsKeys("nombre", "correo", "curp");
    }

    @Test
    @DisplayName("Si un campo tiene varias reglas incumplidas se reporta el primer mensaje")
    void dtoValidation_mismoCampoVariasReglas() {
        MethodArgumentNotValidException ex = excepcion(
                new FieldError("dto", "curp", "La CURP es obligatoria"),
                new FieldError("dto", "curp", "Formato de CURP inválido"));

        ErrorResponse body = (ErrorResponse) validador.dtoValidation(ex, request).getBody();

        assertThat(body.getDetalles()).hasSize(1).containsEntry("curp", "La CURP es obligatoria");
    }
}
