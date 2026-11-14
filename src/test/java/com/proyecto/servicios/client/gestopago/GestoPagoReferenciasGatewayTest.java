package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoReferenciasClient;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia.Estado;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GestoPagoReferenciasGateway")
class GestoPagoReferenciasGatewayTest {

    private static final ProductoCatalogo PRODUCTO = new ProductoCatalogo("1", "100", "Telcel", new BigDecimal("100"));

    private final GestoPagoReferenciasClient client = mock(GestoPagoReferenciasClient.class);
    private final GestoPagoReferenciasGateway gateway = new GestoPagoReferenciasGateway(client, "/consulta.do", Set.of(" 0", "00", ""));

    @Test
    @DisplayName("Sin ruta configurada no consulta y responde NO_DISPONIBLE (falla cerrada)")
    void sinConfiguracion() {
        GestoPagoReferenciasGateway sinRuta = new GestoPagoReferenciasGateway(client, " ", null);
        assertThat(sinRuta.validar(PRODUCTO, "5512345678").estado()).isEqualTo(Estado.NO_DISPONIBLE);
        assertThat(new GestoPagoReferenciasGateway(client, "/", Set.of()).validar(PRODUCTO, "1").codigo()).isEqualTo("SIN_CONFIGURACION");
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("Envía servicio, producto y referencia; código de éxito con monto → VALIDA")
    @SuppressWarnings("unchecked")
    void valida() {
        when(client.consultar(any())).thenReturn("{\"codigo\":\"00\",\"saldo\":\"120.50\"}");
        var r = gateway.validar(PRODUCTO, "5512345678");
        assertThat(r.estado()).isEqualTo(Estado.VALIDA);
        assertThat(r.montoAdeudo()).isEqualByComparingTo("120.50");
        verify(client).consultar((Map<String, ?>) Map.of("idServicio", "1", "idProducto", "100", "referencia", "5512345678"));
    }

    @Test
    @DisplayName("Interpretación de respuestas")
    void interpretar() {
        assertThat(gateway.interpretar("<R><CODIGO>0</CODIGO><MONTO>abc</MONTO></R>").montoAdeudo()).isNull();
        assertThat(gateway.interpretar("<R><CODIGO>0</CODIGO><MONTO>-1</MONTO></R>").montoAdeudo()).isNull();
        assertThat(gateway.interpretar("<R><CODIGO>14</CODIGO></R>").estado()).isEqualTo(Estado.INVALIDA);
        assertThat(gateway.interpretar("<R><TEXTO>x</TEXTO></R>").codigo()).isEqualTo("SIN_CODIGO");
        assertThat(gateway.interpretar("texto").codigo()).isEqualTo("RESPUESTA_ILEGIBLE");
    }

    @Test
    @DisplayName("Errores HTTP o de red → NO_DISPONIBLE")
    void errores() {
        Request request = Request.create(Request.HttpMethod.POST, "/consulta.do", Map.of(), null, StandardCharsets.UTF_8, null);
        when(client.consultar(any())).thenThrow(new FeignException.ServiceUnavailable("x", request, null, Map.of()));
        assertThat(gateway.validar(PRODUCTO, "5512345678").codigo()).isEqualTo("HTTP_503");

        org.mockito.Mockito.doThrow(new IllegalStateException("x")).when(client).consultar(any());
        assertThat(gateway.validar(PRODUCTO, "5512345678").codigo()).isEqualTo("ERROR_IllegalStateException");
    }
}
