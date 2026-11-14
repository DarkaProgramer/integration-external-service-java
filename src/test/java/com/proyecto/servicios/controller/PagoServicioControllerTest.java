package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.ValidadorCampos;
import com.proyecto.servicios.exception.pagos.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.pagos.PagoServicioExceptionHandler;
import com.proyecto.servicios.exception.pagos.SaldoInsuficienteException;
import com.proyecto.servicios.model.pagos.PagoServicioResponseDto;
import com.proyecto.servicios.seguridad.ClienteActualArgumentResolver;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.seguridad.TokenClienteService;
import com.proyecto.servicios.service.pagos.PagoServicioService;
import com.proyecto.servicios.service.pagos.PagoServicioService.ResultadoPagoServicio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PagoServicioController - códigos HTTP y manejo de errores")
class PagoServicioControllerTest {

    private static final String URL = "/api/pagos/servicios";
    private static final String BODY = """
            {"numeroCuenta":"1234567890","idServicio":"1","idProducto":"100","referencia":"5512345678","validacionId":"6f1c2a3b-4d5e-4f60-8a7b-9c0d1e2f3a4b","monto":100.00}""";

    private PagoServicioService pagoServicioService;
    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() {
        pagoServicioService = mock(PagoServicioService.class);
        TokenClienteService tokenService = new TokenClienteService("secreto-de-pruebas-de-al-menos-32-bytes!!",
                "hasani-pagos", Clock.systemUTC());
        token = "Bearer " + tokenService.emitir(1L, List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mockMvc = MockMvcBuilders.standaloneSetup(new PagoServicioController(pagoServicioService))
                .setCustomArgumentResolvers(new ClienteActualArgumentResolver(tokenService, cliente -> { }))
                .setControllerAdvice(new ValidadorCampos(), new PagoServicioExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @ParameterizedTest
    @CsvSource({"APROBADA,201,0", "RECHAZADA,422,1", "EN_CONCILIACION,202,0", "REVISION_MANUAL,202,0"})
    void estatusDelPago_definenElCodigoHttp(EstatusPago estatus, int http, int codigo) throws Exception {
        when(pagoServicioService.pagar(any(), any(), any())).thenReturn(new ResultadoPagoServicio(dto(estatus), false));

        pagar().andExpect(status().is(http))
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.datos.estatus").value(estatus.name()))
                .andExpect(header().string("Idempotent-Replayed", "false"));
    }

    @Test
    void solicitudRepetida_200ConEncabezado() throws Exception {
        when(pagoServicioService.pagar(any(), any(), any())).thenReturn(new ResultadoPagoServicio(dto(EstatusPago.APROBADA), true));

        pagar().andExpect(status().isOk()).andExpect(header().string("Idempotent-Replayed", "true"));
    }

    @Test
    void sinToken_401ConWwwAuthenticate() throws Exception {
        mockMvc.perform(post(URL).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.codigo").value(1));
        verifyNoInteractions(pagoServicioService);
    }

    @Test
    void sinIdempotencyKey_400() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(containsString("Idempotency-Key")));
    }

    @Test
    void camposInvalidos_400ConDetallePorCampo() throws Exception {
        mockMvc.perform(post(URL).header("Authorization", token).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"12\",\"idServicio\":\"a\",\"idProducto\":\"100\",\"referencia\":\"55-12\",\"monto\":10.123}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.numeroCuenta").exists())
                .andExpect(jsonPath("$.detalles.idServicio").exists())
                .andExpect(jsonPath("$.detalles.referencia").exists())
                .andExpect(jsonPath("$.detalles.monto").exists());
        verifyNoInteractions(pagoServicioService);
    }

    @Test
    void errorDeNegocio_usaStatusYMensajeSeguro() throws Exception {
        when(pagoServicioService.pagar(any(), any(), any())).thenThrow(new SaldoInsuficienteException());

        pagar().andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Saldo insuficiente"))
                .andExpect(jsonPath("$.ruta").value(URL));
    }

    @Test
    void catalogoNoDisponible_503() throws Exception {
        when(pagoServicioService.pagar(any(), any(), any())).thenThrow(new CatalogoNoDisponibleException());

        pagar().andExpect(status().isServiceUnavailable());
    }

    @Test
    void errorInesperado_500SinDetallesInternos() throws Exception {
        when(pagoServicioService.pagar(any(), any(), any()))
                .thenThrow(new IllegalStateException("jdbc:mysql://10.0.0.5 password=secreto"));

        pagar().andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value(not(containsString("secreto"))))
                .andExpect(jsonPath("$.mensaje").value(not(containsString("jdbc"))));
    }

    private ResultActions pagar() throws Exception {
        return mockMvc.perform(post(URL).header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(BODY));
    }

    private PagoServicioResponseDto dto(EstatusPago estatus) {
        return PagoServicioResponseDto.builder().folio(UUID.randomUUID().toString()).estatus(estatus)
                .idServicio("1").idProducto("100").referencia("******5678").monto(new BigDecimal("100.00")).build();
    }
}
