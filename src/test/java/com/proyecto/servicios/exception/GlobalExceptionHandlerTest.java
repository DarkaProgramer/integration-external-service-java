package com.proyecto.servicios.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para GlobalExceptionHandler.
 * Verifica que cada tipo de excepción retorna el HTTP status y cuerpo correctos.
 */
@DisplayName("GlobalExceptionHandler - Pruebas Unitarias")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─────────────────────────────────────────────
    //  ClienteYaRegistradoException → 409 CONFLICT
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("ClienteYaRegistradoException → 409 CONFLICT")
    class ClienteYaRegistradoTests {

        @Test
        @DisplayName("Retorna HTTP 409 con el mensaje de la excepción")
        void handle_clienteYaRegistrado_retorna409() {
            ClienteYaRegistradoException ex = new ClienteYaRegistradoException("CURP ya registrada");
            ResponseEntity<Map<String, Object>> resp = handler.handleClienteYaRegistrado(ex);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody()).containsKey("error");
            assertThat(resp.getBody().get("error")).isEqualTo("CURP ya registrada");
        }

        @Test
        @DisplayName("Respuesta incluye campo 'timestamp'")
        void handle_clienteYaRegistrado_incluyeTimestamp() {
            ResponseEntity<Map<String, Object>> resp =
                    handler.handleClienteYaRegistrado(new ClienteYaRegistradoException("msg"));
            assertThat(resp.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Respuesta incluye campo 'status' con valor 409")
        void handle_clienteYaRegistrado_incluyeStatus409() {
            ResponseEntity<Map<String, Object>> resp =
                    handler.handleClienteYaRegistrado(new ClienteYaRegistradoException("msg"));
            assertThat(resp.getBody().get("status")).isEqualTo(409);
        }
    }

    // ─────────────────────────────────────────────
    //  ClienteNoEncontradoException → 404 NOT FOUND
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("ClienteNoEncontradoException → 404 NOT FOUND")
    class ClienteNoEncontradoTests {

        @Test
        @DisplayName("Retorna HTTP 404 con mensaje")
        void handle_clienteNoEncontrado_retorna404() {
            ClienteNoEncontradoException ex = new ClienteNoEncontradoException("Cliente no encontrado con ID: 99");
            ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getBody().get("error")).isEqualTo("Cliente no encontrado con ID: 99");
        }

        @Test
        @DisplayName("Respuesta incluye campo 'status' con valor 404")
        void handle_clienteNoEncontrado_incluyeStatus404() {
            ResponseEntity<Map<String, Object>> resp =
                    handler.handleNotFound(new ClienteNoEncontradoException("msg"));
            assertThat(resp.getBody().get("status")).isEqualTo(404);
        }
    }

    // ─────────────────────────────────────────────
    //  CuentaNoEncontradaException → 404 NOT FOUND
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("CuentaNoEncontradaException → 404 NOT FOUND")
    class CuentaNoEncontradaTests {

        @Test
        @DisplayName("Retorna HTTP 404 con mensaje de cuenta")
        void handle_cuentaNoEncontrada_retorna404() {
            CuentaNoEncontradaException ex = new CuentaNoEncontradaException("Cuenta no encontrada: 0000000000");
            ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(ex);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resp.getBody().get("error").toString()).contains("0000000000");
        }
    }

    // ─────────────────────────────────────────────
    //  MethodArgumentNotValidException → 400
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("MethodArgumentNotValidException → 400 BAD REQUEST")
    class ValidationExceptionTests {

        private MethodArgumentNotValidException buildValidationException(String field, String message) {
            // Crear un binding result con un FieldError
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "clienteRegistroRequestDto");
            bindingResult.addError(new FieldError("clienteRegistroRequestDto", field, message));

            return new MethodArgumentNotValidException(null, bindingResult);
        }

        @Test
        @DisplayName("Retorna HTTP 400 con mapa de errores por campo")
        void handle_validationException_retorna400ConErrores() {
            MethodArgumentNotValidException ex = buildValidationException("nombre", "El nombre es obligatorio");
            ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resp.getBody()).containsKey("messages");

            @SuppressWarnings("unchecked")
            Map<String, String> messages = (Map<String, String>) resp.getBody().get("messages");
            assertThat(messages).containsEntry("nombre", "El nombre es obligatorio");
        }

        @Test
        @DisplayName("Respuesta incluye campo 'error' con texto de validación")
        void handle_validationException_incluyeCampoError() {
            MethodArgumentNotValidException ex = buildValidationException("curp", "CURP inválida");
            ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

            assertThat(resp.getBody().get("error")).isEqualTo("Error de validación");
        }

        @Test
        @DisplayName("Respuesta incluye campo 'timestamp'")
        void handle_validationException_incluyeTimestamp() {
            MethodArgumentNotValidException ex = buildValidationException("rfc", "RFC inválido");
            ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

            assertThat(resp.getBody()).containsKey("timestamp");
        }

        @Test
        @DisplayName("Múltiples errores de validación se incluyen todos")
        void handle_validationException_multiplesErrores() {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "obj");
            bindingResult.addError(new FieldError("obj", "nombre", "Nombre obligatorio"));
            bindingResult.addError(new FieldError("obj", "correo", "Correo inválido"));
            bindingResult.addError(new FieldError("obj", "curp", "CURP inválida"));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

            @SuppressWarnings("unchecked")
            Map<String, String> messages = (Map<String, String>) resp.getBody().get("messages");
            assertThat(messages).hasSize(3);
            assertThat(messages).containsKeys("nombre", "correo", "curp");
        }

        @Test
        @DisplayName("Respuesta incluye status 400 en el body")
        void handle_validationException_incluyeStatus400() {
            MethodArgumentNotValidException ex = buildValidationException("campo", "error");
            ResponseEntity<Map<String, Object>> resp = handler.handleValidationExceptions(ex);

            assertThat(resp.getBody().get("status")).isEqualTo(400);
        }
    }

    // ─────────────────────────────────────────────
    //  Excepciones personalizadas — constructores
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Excepciones personalizadas — constructores")
    class ExcepcionesPersonalizadasTests {

        @Test
        @DisplayName("ClienteYaRegistradoException hereda de RuntimeException")
        void clienteYaRegistradoException_esRuntimeException() {
            assertThat(new ClienteYaRegistradoException("msg"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("ClienteNoEncontradoException hereda de RuntimeException")
        void clienteNoEncontradoException_esRuntimeException() {
            assertThat(new ClienteNoEncontradoException("msg"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("CuentaNoEncontradaException hereda de RuntimeException")
        void cuentaNoEncontradaException_esRuntimeException() {
            assertThat(new CuentaNoEncontradaException("msg"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("El mensaje de excepción se preserva correctamente")
        void excepcion_mensajePreservado() {
            String mensaje = "Error específico de prueba";
            assertThat(new ClienteYaRegistradoException(mensaje).getMessage()).isEqualTo(mensaje);
            assertThat(new ClienteNoEncontradoException(mensaje).getMessage()).isEqualTo(mensaje);
            assertThat(new CuentaNoEncontradaException(mensaje).getMessage()).isEqualTo(mensaje);
        }
    }
}
