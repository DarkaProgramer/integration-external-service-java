package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.comun.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para GlobalExceptionHandler.
 * Verifica el código HTTP y la estructura uniforme de cada error.
 */
@DisplayName("GlobalExceptionHandler - Pruebas Unitarias")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("POST", "/api/clientes");
    }

    @Nested
    @DisplayName("Excepciones de negocio")
    class NegocioTests {

        @Test
        @DisplayName("ClienteYaRegistradoException → 409 con mensaje y ruta")
        void clienteYaRegistrado_409() {
            ResponseEntity<ErrorResponse> resp = handler.handleClienteYaRegistrado(
                    new ClienteYaRegistradoException("CURP ya registrada"), request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody().getStatus()).isEqualTo(409);
            assertThat(resp.getBody().getCodigo()).isEqualTo(1);
            assertThat(resp.getBody().getMensaje()).isEqualTo("CURP ya registrada");
            assertThat(resp.getBody().getRuta()).isEqualTo("/api/clientes");
            assertThat(resp.getBody().getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("CorreoYaRegistradoException → 409")
        void correoYaRegistrado_409() {
            ResponseEntity<ErrorResponse> resp = handler.handleCorreoYaRegistrado(
                    new CorreoYaRegistradoException("Correo ya registrado"), request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody().getError()).isEqualTo("Correo duplicado");
        }

        @Test
        @DisplayName("ClienteNoEncontradoException y CuentaNoEncontradaException → 404")
        void noEncontrado_404() {
            ResponseEntity<ErrorResponse> cliente = handler.handleNotFound(
                    new ClienteNoEncontradoException("No existe un cliente con la CURP o RFC: X"), request);
            ResponseEntity<ErrorResponse> cuenta = handler.handleNotFound(
                    new CuentaNoEncontradaException("Cuenta no encontrada: 0000000000"), request);

            assertThat(cliente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(cuenta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(cuenta.getBody().getMensaje()).contains("0000000000");
        }

        @Test
        @DisplayName("ReglaNegocioException → 422")
        void reglaNegocio_422() {
            ResponseEntity<ErrorResponse> resp = handler.handleReglaNegocio(
                    new ReglaNegocioException("El cliente debe ser mayor de edad"), request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(resp.getBody().getStatus()).isEqualTo(422);
        }
    }

    @Nested
    @DisplayName("Errores técnicos: nunca exponen detalles internos")
    class TecnicosTests {

        @Test
        @DisplayName("DataIntegrityViolationException → 409 sin SQL")
        void integridad_409_sinSql() {
            ResponseEntity<ErrorResponse> resp = handler.handleIntegridadDatos(
                    new DataIntegrityViolationException("Duplicate entry for key 'UK_curp' [insert into clientes ...]"), request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resp.getBody().getMensaje()).doesNotContain("insert", "UK_curp");
        }

        @Test
        @DisplayName("Exception genérica → 500 con mensaje descriptivo y sin detalles")
        void generica_500() {
            ResponseEntity<ErrorResponse> resp = handler.handleErrorInesperado(
                    new NullPointerException("campo x es null en ClienteServiceImpl"), request);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(resp.getBody().getMensaje())
                    .isNotEqualTo("Error")
                    .doesNotContain("ClienteServiceImpl", "null");
        }
    }

    @Nested
    @DisplayName("Excepciones personalizadas")
    class ExcepcionesPersonalizadasTests {

        @Test
        @DisplayName("Son RuntimeException y conservan el mensaje")
        void excepciones_conservanMensaje() {
            String mensaje = "Error específico de prueba";
            assertThat(new ClienteYaRegistradoException(mensaje)).isInstanceOf(RuntimeException.class).hasMessage(mensaje);
            assertThat(new ClienteNoEncontradoException(mensaje)).isInstanceOf(RuntimeException.class).hasMessage(mensaje);
            assertThat(new CuentaNoEncontradaException(mensaje)).isInstanceOf(RuntimeException.class).hasMessage(mensaje);
            assertThat(new CorreoYaRegistradoException(mensaje)).isInstanceOf(RuntimeException.class).hasMessage(mensaje);
            assertThat(new ReglaNegocioException(mensaje)).isInstanceOf(RuntimeException.class).hasMessage(mensaje);
        }
    }
}
