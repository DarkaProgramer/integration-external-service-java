package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
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
    //  POST /clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /clientes")
    class PostClienteTests {

        @Test
        @DisplayName("Registro exitoso retorna 201 CREATED con el cliente")
        void post_datosValidos_retorna201() throws Exception {
            when(clienteService.registrarCliente(any())).thenReturn(buildClienteMock());

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Juan"))
                    .andExpect(jsonPath("$.activo").value(true));
        }

        @Test
        @DisplayName("CURP duplicada retorna 409 CONFLICT")
        void post_curpDuplicada_retorna409() throws Exception {
            when(clienteService.registrarCliente(any()))
                    .thenThrow(new ClienteYaRegistradoException("CURP ya registrada"));

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(containsString("CURP")));
        }

        @Test
        @DisplayName("RFC duplicado retorna 409 CONFLICT")
        void post_rfcDuplicado_retorna409() throws Exception {
            when(clienteService.registrarCliente(any()))
                    .thenThrow(new ClienteYaRegistradoException("RFC ya registrado"));

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Nombre vacío retorna 400 BAD REQUEST con mensaje de validación")
        void post_nombreVacio_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("");

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.ingresoMensual").exists());
        }

        @Test
        @DisplayName("Body vacío retorna 400 con múltiples errores de validación")
        void post_bodyVacio_retorna400() throws Exception {
            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
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

            mockMvc.perform(post("/clientes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.messages.domicilio").exists());
        }
    }

    // ─────────────────────────────────────────────
    //  GET /clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("GET /clientes")
    class GetClientesTests {

        @Test
        @DisplayName("Consulta todos los clientes retorna 200 con lista")
        void get_todos_retorna200() throws Exception {
            when(clienteService.consultarTodosClientes()).thenReturn(List.of(buildClienteMock()));

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nombre").value("Juan"));
        }

        @Test
        @DisplayName("Consulta todos con BD vacía retorna 200 con lista vacía")
        void get_todos_bdVacia_retorna200ListaVacia() throws Exception {
            when(clienteService.consultarTodosClientes()).thenReturn(List.of());

            mockMvc.perform(get("/clientes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("GET /clientes/{id} existente retorna 200")
        void get_porId_existente_retorna200() throws Exception {
            when(clienteService.consultarClientePorId(1L)).thenReturn(buildClienteMock());

            mockMvc.perform(get("/clientes/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("GET /clientes/{id} inexistente retorna 404")
        void get_porId_inexistente_retorna404() throws Exception {
            when(clienteService.consultarClientePorId(99L))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            mockMvc.perform(get("/clientes/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /clientes/curp/{curp} retorna 200 con cliente")
        void get_porCurp_retorna200() throws Exception {
            when(clienteService.consultarClientePorCurp("GALJ900515HJCRPN09"))
                    .thenReturn(buildClienteMock());

            mockMvc.perform(get("/clientes/curp/GALJ900515HJCRPN09"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.curp").value("GALJ900515HJCRPN09"));
        }

        @Test
        @DisplayName("GET /clientes/curp/{curp} inexistente retorna 404")
        void get_porCurp_inexistente_retorna404() throws Exception {
            when(clienteService.consultarClientePorCurp("INVALIDA"))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            mockMvc.perform(get("/clientes/curp/INVALIDA"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /clientes/rfc/{rfc} retorna 200 con cliente")
        void get_porRfc_retorna200() throws Exception {
            when(clienteService.consultarClientePorRfc("GALJ900515AB1"))
                    .thenReturn(buildClienteMock());

            mockMvc.perform(get("/clientes/rfc/GALJ900515AB1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rfc").value("GALJ900515AB1"));
        }

        @Test
        @DisplayName("GET /clientes/rfc/{rfc} inexistente retorna 404")
        void get_porRfc_inexistente_retorna404() throws Exception {
            when(clienteService.consultarClientePorRfc("RFCNOVALIDO"))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            mockMvc.perform(get("/clientes/rfc/RFCNOVALIDO"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /clientes/correo/{correo} retorna 200 con cliente")
        void get_porCorreo_retorna200() throws Exception {
            when(clienteService.consultarClientePorCorreo("juan.garcia@email.com"))
                    .thenReturn(buildClienteMock());

            mockMvc.perform(get("/clientes/correo/juan.garcia@email.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correo").value("juan.garcia@email.com"));
        }

        @Test
        @DisplayName("GET /clientes/activos retorna solo clientes activos")
        void get_activos_retornaActivos() throws Exception {
            Cliente activo = buildClienteMock();
            activo.setActivo(true);
            when(clienteService.consultarClientesActivos()).thenReturn(List.of(activo));

            mockMvc.perform(get("/clientes/activos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].activo").value(true));
        }

        @Test
        @DisplayName("GET /clientes/cuentas/activas retorna cuentas activas")
        void get_cuentasActivas_retornaCuentas() throws Exception {
            Cuenta activa = new Cuenta();
            activa.setId(1L);
            activa.setNumeroCuenta("0000000001");
            activa.setSaldo(BigDecimal.valueOf(1000));
            activa.setEstatus("ACTIVA");
            activa.setCreatedAt(LocalDateTime.now());
            when(clienteService.consultarCuentasActivas()).thenReturn(List.of(activa));

            mockMvc.perform(get("/clientes/cuentas/activas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estatus").value("ACTIVA"));
        }

        @Test
        @DisplayName("GET /clientes/rango-fechas con parámetros válidos retorna 200")
        void get_rangoFechas_retorna200() throws Exception {
            when(clienteService.consultarClientesPorRangoFechas(any(), any()))
                    .thenReturn(List.of(buildClienteMock()));

            mockMvc.perform(get("/clientes/rango-fechas")
                            .param("inicio", "2025-01-01")
                            .param("fin", "2025-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));
        }
    }

    // ─────────────────────────────────────────────
    //  PUT /clientes/{id}
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /clientes/{id}")
    class PutClienteTests {

        @Test
        @DisplayName("Actualización exitosa retorna 200 con cliente actualizado")
        void put_datosValidos_retorna200() throws Exception {
            Cliente actualizado = buildClienteMock();
            actualizado.setNombre("Carlos");
            when(clienteService.actualizarCliente(eq(1L), any())).thenReturn(actualizado);

            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("Carlos");

            mockMvc.perform(put("/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Carlos"));
        }

        @Test
        @DisplayName("Actualización de cliente inexistente retorna 404")
        void put_clienteInexistente_retorna404() throws Exception {
            when(clienteService.actualizarCliente(eq(99L), any()))
                    .thenThrow(new ClienteNoEncontradoException("No encontrado"));

            mockMvc.perform(put("/clientes/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildDtoValido())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Actualización con datos inválidos retorna 400")
        void put_datosInvalidos_retorna400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre(""); // nombre vacío — inválido

            mockMvc.perform(put("/clientes/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────
    //  DELETE /clientes/{id} (baja lógica)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /clientes/{id} (baja lógica)")
    class DeleteClienteTests {

        @Test
        @DisplayName("Baja lógica exitosa retorna 204 NO CONTENT")
        void delete_existente_retorna204() throws Exception {
            doNothing().when(clienteService).bajaLogicaCliente(1L);

            mockMvc.perform(delete("/clientes/1"))
                    .andExpect(status().isNoContent());

            verify(clienteService).bajaLogicaCliente(1L);
        }

        @Test
        @DisplayName("Baja lógica de cliente inexistente retorna 404")
        void delete_inexistente_retorna404() throws Exception {
            doThrow(new ClienteNoEncontradoException("No encontrado"))
                    .when(clienteService).bajaLogicaCliente(99L);

            mockMvc.perform(delete("/clientes/99"))
                    .andExpect(status().isNotFound());
        }
    }
}
