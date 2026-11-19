package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.ResultadoProveedor.Estado;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("GestoPagoPagosGateway - clasificación de resultados de sendTx")
class GestoPagoPagosGatewayTest {

    private static final SolicitudPagoProveedor SOLICITUD =
            new SolicitudPagoProveedor("folio-1", "1", "100", "5512345678", new BigDecimal("100"));

    private GestoPagoTransaccionesClient client;
    private GestoPagoPagosGateway gateway;
    private Request request;

    @BeforeEach
    void setUp() {
        client = mock(GestoPagoTransaccionesClient.class);
        gateway = new GestoPagoPagosGateway(client, Set.of("0", "00"), Set.of("05", " 14 "));
        request = Request.create(Request.HttpMethod.POST, "/sistema/service/sendTx.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void enviaParametrosConMontoNormalizadoYFolioInterno() {
        when(client.sendTx(anyMap())).thenReturn("<R><CODIGO>0</CODIGO></R>");

        gateway.enviarPago(SOLICITUD);

        ArgumentCaptor<Map<String, ?>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).sendTx(captor.capture());
        Map<String, Object> enviados = Map.copyOf(captor.getValue());
        assertThat(enviados).containsEntry("montoPago", "100.00")
                .containsEntry("idTransaccionCliente", "folio-1")
                .containsEntry("referencia", "5512345678");
    }

    @Test
    void codigoDeExito_aprobadaConFolioDelProveedor() {
        when(client.sendTx(anyMap())).thenReturn("<R><CODIGO>00</CODIGO><TRANSACCION>GP-9</TRANSACCION></R>");

        ResultadoProveedor r = gateway.enviarPago(SOLICITUD);

        assertThat(r.estado()).isEqualTo(Estado.APROBADA);
        assertThat(r.folioProveedor()).isEqualTo("GP-9");
    }

    @Test
    void codigoDeRechazoConfigurado_rechazada() {
        when(client.sendTx(anyMap())).thenReturn("{\"codigo\":\"14\"}");

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.RECHAZADA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"codigo\":\"99\"}", "{\"otro\":1}", "texto plano", ""})
    void codigoDesconocidoOSinCodigoOIlegible_indeterminada(String cuerpo) {
        when(client.sendTx(anyMap())).thenReturn(cuerpo);

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.INDETERMINADA);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422, 429})
    void httpQueIndicaNoProcesado_rechazada(int status) {
        when(client.sendTx(anyMap())).thenThrow(feignError(status));

        ResultadoProveedor r = gateway.enviarPago(SOLICITUD);

        assertThat(r.estado()).isEqualTo(Estado.RECHAZADA);
        assertThat(r.codigo()).isEqualTo("HTTP_" + status);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 409, 500, 502, 503, 504})
    void httpDudoso_indeterminada(int status) {
        when(client.sendTx(anyMap())).thenThrow(feignError(status));

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.INDETERMINADA);
    }

    @Test
    void timeoutDeLectura_indeterminadaNuncaRechazada() {
        when(client.sendTx(anyMap())).thenThrow(retryable(new SocketTimeoutException("Read timed out")));

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.INDETERMINADA);
    }

    @Test
    void conexionCortadaDespuesDeEnviar_indeterminada() {
        when(client.sendTx(anyMap())).thenThrow(retryable(new SocketException("Connection reset")));

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.INDETERMINADA);
    }

    @Test
    void conexionRechazadaOHostDesconocido_rechazadaPorqueNuncaSalio() {
        when(client.sendTx(anyMap())).thenThrow(retryable(new ConnectException("Connection refused")));
        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.RECHAZADA);

        doThrow(retryable(new UnknownHostException("gestopago"))).when(client).sendTx(anyMap());
        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.RECHAZADA);
    }

    @Test
    void tokenNoConfigurado_rechazadaSinEnviar() {
        when(client.sendTx(anyMap())).thenThrow(new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION));

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.RECHAZADA);
    }

    @Test
    void errorInesperado_indeterminada() {
        when(client.sendTx(anyMap())).thenThrow(new IllegalStateException("x"));

        assertThat(gateway.enviarPago(SOLICITUD).estado()).isEqualTo(Estado.INDETERMINADA);
    }

    @Test
    void consultarEstado_sinEndpointDelProveedor_devuelveVacio() {
        assertThat(gateway.consultarEstado(SOLICITUD)).isEmpty();
    }

    @Test
    void consultarEstado_conRuta_aceptaSoloCodigosConfigurados() {
        GestoPagoPagosGateway conConsulta = new GestoPagoPagosGateway(client, Set.of("0", "00"), Set.of("05"), "/consulta.do");
        when(client.consultarTx(anyMap()))
                .thenReturn("<R><CODIGO>0</CODIGO><FOLIO>GP-9</FOLIO></R>")
                .thenReturn("<R><CODIGO>99</CODIGO></R>");

        assertThat(conConsulta.consultarEstado(SOLICITUD)).get()
                .extracting(ResultadoProveedor::estado).isEqualTo(ResultadoProveedor.Estado.APROBADA);
        // Código desconocido: no se decide nada
        assertThat(conConsulta.consultarEstado(SOLICITUD)).isEmpty();
    }

    @Test
    void consultarEstado_errorDeRed_noCambiaNada() {
        GestoPagoPagosGateway conConsulta = new GestoPagoPagosGateway(client, Set.of("0"), Set.of("05"), "/consulta.do");
        when(client.consultarTx(anyMap())).thenThrow(feignError(500));

        assertThat(conConsulta.consultarEstado(SOLICITUD)).isEmpty();
    }

    private FeignException feignError(int status) {
        Response response = Response.builder().status(status).reason("x").request(request)
                .headers(Collections.emptyMap()).build();
        return FeignException.errorStatus("sendTx", response);
    }

    private RetryableException retryable(Throwable cause) {
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.POST, cause, (Long) null, request);
    }
}
