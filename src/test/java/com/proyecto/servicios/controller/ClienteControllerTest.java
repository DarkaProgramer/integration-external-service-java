package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CorreoYaRegistradoException;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.exception.ValidadorCampos;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas del controlador ClienteController con MockMvc (slice del controlador + manejador global).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteController - Pruebas de API REST")
class ClienteControllerTest {

    private static final String URL = "/api/clientes";
    private static final String CURP = "GALJ900515HJCRPN09";

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new ValidadorCampos(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
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
        dto.setCurp(CURP);
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

    private ClienteResponseDto buildResponse(String nombre) {
        return ClienteResponseDto.builder()
                .nombre(nombre)
                .apellidoPaterno("García")
                .curp(CURP)
                .rfc("GALJ900515AB1")
                .correo("juan.garcia@email.com")
                .activo(true)
                .build();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ─────────────────────────────────────────────
    //  POST /api/clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/clientes")
    class RegistroTests {

        @Test
        @DisplayName("Registro exitoso retorna 201 con la estructura uniforme y sin id")
        void registro_valido_201() throws Exception {
            when(clienteService.registrar(any())).thenReturn(buildResponse("Juan"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildDtoValido())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value(0))
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.mensaje").value("Cliente registrado correctamente"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.datos.nombre").value("Juan"))
                    .andExpect(jsonPath("$.datos.curp").value(CURP))
                    .andExpect(jsonPath("$.datos.id").doesNotExist());
        }

        @Test
        @DisplayName("CURP duplicada retorna 409")
        void registro_curpDuplicada_409() throws Exception {
            when(clienteService.registrar(any())).thenThrow(new ClienteYaRegistradoException("Ya existe un cliente con la CURP"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildDtoValido())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value(1))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.mensaje").value(containsString("CURP")))
                    .andExpect(jsonPath("$.ruta").value(URL));
        }

        @Test
        @DisplayName("Correo duplicado retorna 409")
        void registro_correoDuplicado_409() throws Exception {
            when(clienteService.registrar(any())).thenThrow(new CorreoYaRegistradoException("El correo ya está registrado"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildDtoValido())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("Correo duplicado"));
        }

        @Test
        @DisplayName("Menor de edad retorna 422")
        void registro_menorDeEdad_422() throws Exception {
            when(clienteService.registrar(any())).thenThrow(new ReglaNegocioException("El cliente debe ser mayor de edad"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildDtoValido())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.mensaje").value(containsString("mayor de edad")));
        }

        @Test
        @DisplayName("Nombre vacío retorna 400 con el detalle del campo")
        void registro_nombreVacio_400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("");

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value(1))
                    .andExpect(jsonPath("$.mensaje").value(containsString("Datos no válidos")))
                    .andExpect(jsonPath("$.error").value("Error de validación"))
                    .andExpect(jsonPath("$.detalles.nombre").exists());
            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("CURP, correo y teléfono inválidos se reportan juntos")
        void registro_variosCamposInvalidos_400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCurp("CURPINVALIDA");
            dto.setCorreo("correo-invalido");
            dto.setTelefonoMovil("331234567");

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.curp").exists())
                    .andExpect(jsonPath("$.detalles.correo").exists())
                    .andExpect(jsonPath("$.detalles.telefonoMovil").exists());
        }

        @Test
        @DisplayName("Código postal de 4 dígitos en el domicilio anidado retorna 400")
        void registro_codigoPostalInvalido_400() throws Exception {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCodigoPostal("4410");

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles['domicilio.codigoPostal']").exists());
        }

        @Test
        @DisplayName("Body vacío retorna 400 con varios errores")
        void registro_bodyVacio_400() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles").isMap())
                    .andExpect(jsonPath("$.detalles.curp").exists())
                    .andExpect(jsonPath("$.detalles.domicilio").exists());
        }

        @Test
        @DisplayName("Enviar campos controlados por el servidor (id, activo) retorna 400")
        void registro_campoNoPermitido_400() throws Exception {
            String body = json(buildDtoValido()).replaceFirst("\\{", "{\"activo\":false,");

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Campo no permitido"))
                    .andExpect(jsonPath("$.mensaje").value(containsString("activo")));
            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("JSON mal formado retorna 400 sin detalles técnicos")
        void registro_jsonMalFormado_400() throws Exception {
            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Solicitud mal formada"))
                    .andExpect(jsonPath("$.mensaje").value(not(containsString("Exception"))));
        }

        @Test
        @DisplayName("Error inesperado retorna 500 con mensaje descriptivo y sin traza")
        void registro_errorInesperado_500() throws Exception {
            when(clienteService.registrar(any())).thenThrow(new IllegalStateException("could not execute statement; SQL [insert into clientes ...]"));

            mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildDtoValido())))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Error interno del servidor"))
                    .andExpect(jsonPath("$.mensaje").value(not(containsString("SQL"))))
                    .andExpect(jsonPath("$.trace").doesNotExist());
        }
    }

    // ─────────────────────────────────────────────
    //  POST /api/clientes/buscar
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/clientes/buscar")
    class BusquedaTests {

        @Test
        @DisplayName("Filtros combinados se envían al servicio y retorna 200")
        void buscar_filtrosCombinados_200() throws Exception {
            when(clienteService.buscar(any())).thenReturn(List.of(buildResponse("Juan")));

            ClienteConsultaRequestDto filtros = new ClienteConsultaRequestDto();
            filtros.setNombre("jua");
            filtros.setActivo(true);
            filtros.setEstado("Jalisco");

            mockMvc.perform(post(URL + "/buscar").contentType(MediaType.APPLICATION_JSON).content(json(filtros)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos", hasSize(1)))
                    .andExpect(jsonPath("$.datos[0].nombre").value("Juan"))
                    .andExpect(jsonPath("$.datos[0].id").doesNotExist());

            ArgumentCaptor<ClienteConsultaRequestDto> captor = ArgumentCaptor.forClass(ClienteConsultaRequestDto.class);
            verify(clienteService).buscar(captor.capture());
            assertThat(captor.getValue().getNombre()).isEqualTo("jua");
            assertThat(captor.getValue().getActivo()).isTrue();
            assertThat(captor.getValue().getEstado()).isEqualTo("Jalisco");
        }

        @Test
        @DisplayName("Sin filtros retorna 200 con todos")
        void buscar_sinFiltros_200() throws Exception {
            when(clienteService.buscar(any())).thenReturn(List.of());

            mockMvc.perform(post(URL + "/buscar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos", hasSize(0)))
                    .andExpect(jsonPath("$.mensaje").value(containsString("No se encontraron")));
        }

        @Test
        @DisplayName("Rango de fechas invertido retorna 400")
        void buscar_rangoFechasInvertido_400() throws Exception {
            ClienteConsultaRequestDto filtros = new ClienteConsultaRequestDto();
            filtros.setFechaRegistroDesde(LocalDate.of(2025, 12, 31));
            filtros.setFechaRegistroHasta(LocalDate.of(2025, 1, 1));

            mockMvc.perform(post(URL + "/buscar").contentType(MediaType.APPLICATION_JSON).content(json(filtros)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.rangoFechasValido").exists());
            verifyNoInteractions(clienteService);
        }

        @Test
        @DisplayName("Filtro con formato inválido retorna 400")
        void buscar_curpInvalida_400() throws Exception {
            ClienteConsultaRequestDto filtros = new ClienteConsultaRequestDto();
            filtros.setCurp("abc");

            mockMvc.perform(post(URL + "/buscar").contentType(MediaType.APPLICATION_JSON).content(json(filtros)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.curp").exists());
        }

        @Test
        @DisplayName("GET sobre /buscar no está permitido (405)")
        void buscar_get_405() throws Exception {
            mockMvc.perform(get(URL + "/buscar"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));
        }
    }

    // ─────────────────────────────────────────────
    //  PUT /api/clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/clientes")
    class ActualizarTests {

        private ClienteActualizarRequestDto buildActualizacion() {
            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscal(CURP);
            dto.setNombre("Carlos");
            dto.setCorreo("carlos@email.com");
            return dto;
        }

        @Test
        @DisplayName("Actualización exitosa retorna 200")
        void actualizar_valido_200() throws Exception {
            when(clienteService.actualizar(any())).thenReturn(buildResponse("Carlos"));

            mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildActualizacion())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value("Cliente actualizado correctamente"))
                    .andExpect(jsonPath("$.datos.nombre").value("Carlos"));
        }

        @Test
        @DisplayName("Cliente inexistente retorna 404")
        void actualizar_inexistente_404() throws Exception {
            when(clienteService.actualizar(any())).thenThrow(new ClienteNoEncontradoException("No existe un cliente con la CURP o RFC"));

            mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json(buildActualizacion())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Recurso no encontrado"));
        }

        @Test
        @DisplayName("Nombre vacío retorna 400")
        void actualizar_nombreVacio_400() throws Exception {
            ClienteActualizarRequestDto dto = buildActualizacion();
            dto.setNombre("");

            mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.nombre").exists());
        }

        @Test
        @DisplayName("Sólo el identificador, sin datos a actualizar, retorna 400")
        void actualizar_sinCampos_400() throws Exception {
            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscal(CURP);

            mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(json(dto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.algunCampoParaActualizar").exists());
        }

        @Test
        @DisplayName("Intentar modificar la CURP retorna 400 (campo no permitido)")
        void actualizar_intentaCambiarCurp_400() throws Exception {
            String body = "{\"identificadorFiscal\":\"" + CURP + "\",\"curp\":\"XXXX900515HJCRPN09\"}";

            mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value(containsString("curp")));
        }
    }

    // ─────────────────────────────────────────────
    //  DELETE /api/clientes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/clientes")
    class BajaTests {

        @Test
        @DisplayName("Baja exitosa retorna 200 con el cliente inactivo")
        void baja_existente_200() throws Exception {
            ClienteResponseDto inactivo = buildResponse("Juan");
            inactivo.setActivo(false);
            when(clienteService.darDeBaja(CURP)).thenReturn(inactivo);

            ClienteEliminarRequestDto request = new ClienteEliminarRequestDto();
            request.setIdentificadorFiscal(CURP);

            mockMvc.perform(delete(URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.datos.activo").value(false));
            verify(clienteService).darDeBaja(CURP);
        }

        @Test
        @DisplayName("Cliente inexistente retorna 404")
        void baja_inexistente_404() throws Exception {
            when(clienteService.darDeBaja(CURP)).thenThrow(new ClienteNoEncontradoException("No encontrado"));

            ClienteEliminarRequestDto request = new ClienteEliminarRequestDto();
            request.setIdentificadorFiscal(CURP);

            mockMvc.perform(delete(URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Identificador con formato inválido retorna 400")
        void baja_identificadorInvalido_400() throws Exception {
            ClienteEliminarRequestDto request = new ClienteEliminarRequestDto();
            request.setIdentificadorFiscal("NOEXISTE");

            mockMvc.perform(delete(URL).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.identificadorFiscal").exists());
        }
    }
}
