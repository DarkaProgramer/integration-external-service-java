package com.proyecto.servicios.client.gestopago;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RespuestaGestoPago - lectura de XML/JSON del proveedor")
class RespuestaGestoPagoTest {

    @Test
    void xml_leeAtributosYElementosSimples() {
        RespuestaGestoPago r = RespuestaGestoPago.leer("""
                <RESPONSE><MENSAJE><CODIGO>0</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
                <PRODUCTOS><producto idServicio="1" idProducto="100" precio="100.0"/></PRODUCTOS></RESPONSE>""");

        assertThat(r.buscar("codigo")).contains("0");
        assertThat(r.buscarNodos(n -> n.valor("idProducto").isPresent()))
                .singleElement().satisfies(p -> assertThat(p.valor("precio")).contains("100.0"));
    }

    @Test
    void json_buscaClavesSinDistinguirMayusculasEnCualquierNivel() {
        RespuestaGestoPago r = RespuestaGestoPago.leer("{\"data\":{\"Codigo\":\"00\",\"Folio\":\"A1\"}}");

        assertThat(r.buscar("CODIGO")).contains("00");
        assertThat(r.buscar("noExiste", "folio")).contains("A1");
    }

    @Test
    void xml_conDoctype_seRechazaParaEvitarXxe() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE r [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <r><CODIGO>&x;</CODIGO></r>""";

        assertThatThrownBy(() -> RespuestaGestoPago.leer(xxe))
                .isInstanceOf(RespuestaGestoPago.RespuestaIlegibleException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"OK", "<sin-cerrar>", "{json roto"})
    void contenidoIlegible_lanzaExcepcionSinIncluirElContenido(String cuerpo) {
        assertThatThrownBy(() -> RespuestaGestoPago.leer(cuerpo))
                .isInstanceOf(RespuestaGestoPago.RespuestaIlegibleException.class)
                .hasMessage("Respuesta del proveedor ilegible");
    }
}
