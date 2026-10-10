package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración del controlador ClienteController con MockMvc.
 * No levanta el contexto completo de Spring — solo el slice del controlador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteController - Pruebas de API REST")
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private ClienteRegistroRequestDto buildDtoValido() {
        DomicilioDto dom = new DomicilioDto();
        dom.setCalle("Av. Revolución");
        dom.setNumeroExterior("123");
        dom.setColonia("Centro");
        dom.setMunicipio("Guadalajara");
        dom.setEstado("Jalisco");
        dom.setCodigoPostal("44100");
        dom.setPais("México");

        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setNombre("Juan");
        dto.setApellidoPaterno("García");
        dto.setApellidoMaterno("López");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        dto.setCurp("GALJ900515HJCRPN09");
        dto.setRfc("GALJ900515AB1");
        dto.setSexo("M");
        dto.setNacionalidad("Mexicana");
        dto.setEstadoCivil("Soltero");
        dto.setCorreo("juan.garcia@email.com");
        dto.setTelefonoMovil("3312345678");
        dto.setOcupacion("Ingeniero");
        dto.setEmpresa("TechCorp");
        dto.setIngresoMensual(BigDecimal.valueOf(20000));
        dto.setDomicilio(dom);
        return dto;
    }

    private ClienteResponseDto buildResponseDtoMock() {
        return new ClienteResponseDto("Juan", "juan.garcia@email.com", "3312345678", "GALJ900515HJCRPN09", "GALJ900515AB1");
    }

    private Cliente buildClienteMock() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNombre("Juan");
        c.setApellidoPaterno("García");
        c.setApellidoMaterno("López");
        c.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        c.setCurp("GALJ900515HJCRPN09");
        c.setRfc("GALJ900515AB1");
        c.setSexo("M");
        c.setNacionalidad("Mexicana");
        c.setEstadoCivil("Soltero");
        c.setCorreo("juan.garcia@email.com");
        c.setTelefonoMovil("3312345678");
        c.setOcupacion("Ingeniero");
        c.setEmpresa("TechCorp");
        c.setIngresoMensual(BigDecimal.valueOf(20000));
        c.setActivo(true);
        c.setCreatedAt(LocalDateTime.now());

        Domicilio dom = new Domicilio();
        dom.setId(1L);
        dom.setCalle("Av. Revolución");
        dom.setNumeroExterior("123");
        dom.setColonia("Centro");
        dom.setMunicipio("Guadalajara");
        dom.setEstado("Jalisco");
        dom.setCodigoPostal("44100");
        dom.setPais("México");
        c.setDomicilio(dom);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("0000000001");
        cuenta.setSaldo(BigDecimal.valueOf(1000));
        cuenta.setEstatus("ACTIVA");
        cuenta.setCreatedAt(LocalDateTime.now());
        c.setCuenta(cuenta);

        return c;
    }

    // ─────────────────────────────────────────────
    //  POST /api/clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/clientes")
    class PostClienteTests {

        @Test
        @DisplayName("Registro exitoso retorna 201 CREATED con el cliente sin ID")
        void post_datosValidos_retorna201() throws Exception {
            when(clienteService.registrar(any())).thenReturn(buildResponseDtoMock());

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Juan"))
                    .andExpect(jsonPath("$.curp").value("GALJ900515HJCRPN09"))
                    .andExpect(jsonPath("$.id").doesNotExist());
        }

        @Test
        @DisplayName("CURP duplicada retorna 409 CONFLICT")
        void post_curpDuplicada_retorna409() throws Exception {
            when(clienteService.registrar(any()))
                    .thenThrow(new ClienteYaRegistradoException("CURP ya registrada"));

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(containsString("CURP")));
        }

        @Test
        @DisplayName("RFC duplicado retorna 409 CONFLICT")
        void post_rfcDuplicado_retorna409() throws Exception {
            when(clienteService.registrar(any()))
                    .thenThrow(new ClienteYaRegistradoException("RFC ya registrado"));

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Nombre vacío retorna 400 BAD REQUEST con mensaje de validación")
        void post_nombreVacio_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("");

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.nombre").exists());
        }

        @Test
        @DisplayName("CURP con formato inválido retorna 400 con mensaje de validación")
        void post_curpInvalida_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCurp("CURPINVALIDA");

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.curp").exists());
        }

        @Test
        @DisplayName("Correo con formato inválido retorna 400")
        void post_correoInvalido_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCorreo("correo-invalido");

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.correo").exists());
        }

        @Test
        @DisplayName("Teléfono con 9 dígitos retorna 400")
        void post_telefono9digitos_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoMovil("331234567");

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.telefonoMovil").exists());
        }

        @Test
        @DisplayName("Código postal con 4 dígitos retorna 400")
        void post_codigoPostal4digitos_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCodigoPostal("4410");

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages['domicilio.codigoPostal']").exists());
        }

        @Test
        @DisplayName("Ingreso mensual de cero retorna 400")
        void post_ingresoCero_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(BigDecimal.ZERO);

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.ingresoMensual").exists());
        }

        @Test
        @DisplayName("Body vacío retorna 400 con múltiples errores de validación")
        void post_bodyVacio_retorna400() throws Exception {
            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages").isMap());
        }

        @Test
        @DisplayName("Fecha de nacimiento futura retorna 400")
        void post_fechaNacimientoFutura_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().plusDays(1));

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.fechaNacimiento").exists());
        }

        @Test
        @DisplayName("Domicilio nulo retorna 400")
        void post_domicilioNulo_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setDomicilio(null);

            mockMvc.perform(post("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.domicilio").exists());
        }
    }

    // ─────────────────────────────────────────────
    //  GET /api/clientes (Consultas generales y por RequestBody)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/clientes")
    class GetClientesTests {

        @Test
        @DisplayName("Consulta todos los clientes retorna 200 con lista")
        void get_todos_retorna200() throws Exception {
            when(clienteService.consultarTodosClientes()).thenReturn(List.of(buildClienteMock()));

            mockMvc.perform(get("/api/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombre").value("Juan"));
        }

        @Test
        @DisplayName("Consulta todos con BD vacía retorna 200 con lista vacía")
        void get_todos_bdVacia_retorna200ListaVacia() throws Exception {
            when(clienteService.consultarTodosClientes()).thenReturn(List.of());

            mockMvc.perform(get("/api/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /api/clientes/buscar existente mediante RequestBody retorna 200 sin ID")
        void get_porCurpRfc_existente_retorna200() throws Exception {
            when(clienteService.buscarPorCurpRfc("GALJ900515HJCRPN09"))
                    .thenReturn(buildResponseDtoMock());

            ClienteConsultaRequestDto request = new ClienteConsultaRequestDto();
            request.setIdentificadorFiscal("GALJ900515HJCRPN09");

            mockMvc.perform(get("/api/clientes/buscar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.curp").value("GALJ900515HJCRPN09"))
                    .andExpect(jsonPath("$.id").doesNotExist());
        }

        @Test
        @DisplayName("GET /api/clientes/buscar inexistente mediante RequestBody retorna 404")
        void get_porCurpRfc_inexistente_retorna404() throws Exception {
            when(clienteService.buscarPorCurpRfc("INVALIDA"))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            ClienteConsultaRequestDto request = new ClienteConsultaRequestDto();
            request.setIdentificadorFiscal("INVALIDA");

            mockMvc.perform(get("/api/clientes/buscar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/clientes/activos retorna solo clientes activos")
        void get_activos_retornaActivos() throws Exception {
            Cliente activo = buildClienteMock();
            activo.setActivo(true);
            when(clienteService.consultarClientesActivos()).thenReturn(List.of(activo));

            mockMvc.perform(get("/api/clientes/activos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].activo").value(true));
        }

        @Test
        @DisplayName("GET /api/clientes/cuentas/activas retorna cuentas activas")
        void get_cuentasActivas_retornaCuentas() throws Exception {
            Cuenta activa = new Cuenta();
            activa.setId(1L);
            activa.setNumeroCuenta("0000000001");
            activa.setSaldo(BigDecimal.valueOf(1000));
            activa.setEstatus("ACTIVA");
            activa.setCreatedAt(LocalDateTime.now());
            when(clienteService.consultarCuentasActivas()).thenReturn(List.of(activa));

            mockMvc.perform(get("/api/clientes/cuentas/activas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estatus").value("ACTIVA"));
        }

        @Test
        @DisplayName("GET /api/clientes/rango-fechas con parámetros válidos retorna 200")
        void get_rangoFechas_retorna200() throws Exception {
            when(clienteService.consultarClientesPorRangoFechas(any(), any()))
                    .thenReturn(List.of(buildClienteMock()));

            mockMvc.perform(get("/api/clientes/rango-fechas")
                            .param("inicio", "2025-01-01")
                            .param("fin", "2025-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // ─────────────────────────────────────────────
    //  PUT /api/clientes (Actualizar mediante CURP o RFC en RequestBody)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/clientes")
    class PutClienteTests {

        @Test
        @DisplayName("Actualización exitosa retorna 200 con cliente actualizado sin ID")
        void put_datosValidos_retorna200() throws Exception {
            ClienteResponseDto actualizado = new ClienteResponseDto("Carlos", "carlos@email.com", "3312345678", "GALJ900515HJCRPN09", "GALJ900515AB1");
            when(clienteService.actualizarPorCurpRfc(any())).thenReturn(actualizado);

            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscalBusqueda("GALJ900515HJCRPN09");
            dto.setNombre("Carlos");
            dto.setCorreo("carlos@email.com");
            dto.setTelefono("3312345678");

            mockMvc.perform(put("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Carlos"))
                    .andExpect(jsonPath("$.id").doesNotExist());
        }

        @Test
        @DisplayName("Actualización de cliente con CURP o RFC inexistente retorna 404")
        void put_clienteInexistente_retorna404() throws Exception {
            when(clienteService.actualizarPorCurpRfc(any()))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscalBusqueda("NOEXISTE");
            dto.setNombre("Carlos");
            dto.setCorreo("carlos@email.com");
            dto.setTelefono("3312345678");

            mockMvc.perform(put("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Actualización con datos inválidos retorna 400")
        void put_datosInvalidos_retorna400() throws Exception {
            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscalBusqueda("GALJ900515HJCRPN09");
            dto.setNombre(""); // nombre vacío — inválido

            mockMvc.perform(put("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    //  DELETE /api/clientes (Baja mediante CURP o RFC en RequestBody)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/clientes")
    class DeleteClienteTests {

        @Test
        @DisplayName("Baja exitosa retorna 204 NO CONTENT")
        void delete_existente_retorna204() throws Exception {
            doNothing().when(clienteService).eliminarPorCurpRfc("GALJ900515HJCRPN09");

            ClienteEliminarRequestDto request = new ClienteEliminarRequestDto();
            request.setIdentificadorFiscal("GALJ900515HJCRPN09");

            mockMvc.perform(delete("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            verify(clienteService).eliminarPorCurpRfc("GALJ900515HJCRPN09");
        }

        @Test
        @DisplayName("Baja de cliente inexistente retorna 404")
        void delete_inexistente_retorna404() throws Exception {
            doThrow(new ClienteNoEncontradoException("No encontrado"))
                    .when(clienteService).eliminarPorCurpRfc("NOEXISTE");

            ClienteEliminarRequestDto request = new ClienteEliminarRequestDto();
            request.setIdentificadorFiscal("NOEXISTE");

            mockMvc.perform(delete("/api/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}