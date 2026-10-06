package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.GestoPagoProduct;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.codec.DecodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestoPagoProductServiceImplTest {

    @Mock
    private GestoPagoProductsClient productsClient;

    @InjectMocks
    private GestoPagoProductServiceImpl service;

    private Request request;

    @BeforeEach
    void setUp() {
        request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }

    @Test
    void obtenerProductos_respuestaExitosa_devuelveListaDelCliente() {
        GestoPagoProduct producto = new GestoPagoProduct();
        producto.setId("1");
        producto.setName("Recarga");
        GestoPagoProductListResponse esperado = new GestoPagoProductListResponse();
        esperado.setProductList(List.of(producto));
        when(productsClient.getProductList()).thenReturn(esperado);

        GestoPagoProductListResponse resultado = service.obtenerProductos();

        assertThat(resultado).isSameAs(esperado);
        assertThat(resultado.getProductList()).hasSize(1);
        verify(productsClient).getProductList();
    }

    @Test
    void obtenerProductos_respuestaSinListaDeProductos_noFalla() {
        when(productsClient.getProductList()).thenReturn(new GestoPagoProductListResponse());

        GestoPagoProductListResponse resultado = service.obtenerProductos();

        assertThat(resultado.getProductList()).isNull();
    }

    @Test
    void obtenerProductos_respuestaNula_lanzaInvalidResponse() {
        when(productsClient.getProductList()).thenReturn(null);

        assertErrorType(GestoPagoErrorType.INVALID_RESPONSE);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403})
    void obtenerProductos_errorDeAutenticacion_lanzaAuthentication(int status) {
        when(productsClient.getProductList()).thenThrow(feignError(status));

        assertErrorType(GestoPagoErrorType.AUTHENTICATION);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    void obtenerProductos_respuestaNoExitosa_lanzaUnsuccessfulResponse(int status) {
        when(productsClient.getProductList()).thenThrow(feignError(status));

        assertErrorType(GestoPagoErrorType.UNSUCCESSFUL_RESPONSE);
    }

    @Test
    void obtenerProductos_timeoutDeLectura_lanzaTimeout() {
        when(productsClient.getProductList())
                .thenThrow(retryable(new SocketTimeoutException("Read timed out")));

        assertErrorType(GestoPagoErrorType.TIMEOUT);
    }

    @Test
    void obtenerProductos_errorDeConexion_lanzaCommunication() {
        when(productsClient.getProductList())
                .thenThrow(retryable(new ConnectException("Connection refused")));

        assertErrorType(GestoPagoErrorType.COMMUNICATION);
    }

    @Test
    void obtenerProductos_respuestaNoDeserializable_lanzaInvalidResponse() {
        when(productsClient.getProductList())
                .thenThrow(new DecodeException(200, "JSON invalido", request, new RuntimeException("parse")));

        assertErrorType(GestoPagoErrorType.INVALID_RESPONSE);
    }

    @Test
    void obtenerProductos_excepcionDeIntegracionDelCliente_sePropagaSinCambios() {
        GestoPagoIntegrationException original =
                new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION);
        when(productsClient.getProductList()).thenThrow(original);

        assertThatThrownBy(() -> service.obtenerProductos()).isSameAs(original);
    }

    @Test
    void obtenerProductos_errorTraducido_noExponeMensajeInternoDelCliente() {
        when(productsClient.getProductList())
                .thenThrow(retryable(new ConnectException("host-interno:8443 refused")));

        assertThatThrownBy(() -> service.obtenerProductos())
                .isInstanceOf(GestoPagoIntegrationException.class)
                .hasMessage(GestoPagoErrorType.COMMUNICATION.getMensaje())
                .hasMessageNotContaining("host-interno");
    }

    private void assertErrorType(GestoPagoErrorType esperado) {
        assertThatThrownBy(() -> service.obtenerProductos())
                .isInstanceOfSatisfying(GestoPagoIntegrationException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(esperado));
    }

    private FeignException feignError(int status) {
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
        return FeignException.errorStatus("getProductList", response);
    }

    private RetryableException retryable(Throwable cause) {
        return new RetryableException(-1, cause.getMessage(), Request.HttpMethod.GET, cause, (Long) null, request);
    }
}
