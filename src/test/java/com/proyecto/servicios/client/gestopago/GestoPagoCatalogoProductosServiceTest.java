package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.exception.pagos.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.pagos.MontoNoPermitidoException;
import com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Catálogo de productos - validación de producto y monto")
class GestoPagoCatalogoProductosServiceTest {

    private static final String CATALOGO = """
            <RESPONSE><PRODUCTOS>
              <producto producto="Telcel $100" idServicio="1" idProducto="100" precio="100.00"/>
              <producto producto="CFE" idServicio="5" idProducto="500" precio="0"/>
              <producto producto="Roto" idServicio="7" idProducto="700" precio="abc"/>
            </PRODUCTOS></RESPONSE>""";

    private GestoPagoProductsClient client;
    private MutableClock clock;
    private GestoPagoCatalogoProductosService service;

    @BeforeEach
    void setUp() {
        client = mock(GestoPagoProductsClient.class);
        clock = new MutableClock(Instant.parse("2026-10-29T10:00:00Z"));
        PagosProperties properties = new PagosProperties();
        properties.setMontoMinimo(new BigDecimal("10.00"));
        properties.setMontoMaximo(new BigDecimal("5000.00"));
        properties.getCatalogo().setTtlMinutos(30);
        service = new GestoPagoCatalogoProductosService(client, properties, clock);
        when(client.getProductListRaw()).thenReturn(CATALOGO);
    }

    @Test
    void precioFijo_montoIgual_devuelveProductoDelCatalogo() {
        ProductoCatalogo p = service.validarProductoYMonto("1", "100", new BigDecimal("100"));

        assertThat(p.nombre()).isEqualTo("Telcel $100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"99.99", "100.01", "1", "1000"})
    void precioFijo_montoDistinto_rechaza(String monto) {
        assertThatThrownBy(() -> service.validarProductoYMonto("1", "100", new BigDecimal(monto)))
                .isInstanceOf(MontoNoPermitidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9.99", "5000.01", "0", "-10", "10.001"})
    void montoVariable_fueraDeRangoOInvalido_rechaza(String monto) {
        assertThatThrownBy(() -> service.validarProductoYMonto("5", "500", new BigDecimal(monto)))
                .isInstanceOf(MontoNoPermitidoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "250.50", "5000.00"})
    void montoVariable_dentroDeRango_acepta(String monto) {
        assertThat(service.validarProductoYMonto("5", "500", new BigDecimal(monto)).idProducto()).isEqualTo("500");
    }

    @Test
    void productoInexistente_oServicioQueNoCorresponde_rechaza() {
        assertThatThrownBy(() -> service.validarProductoYMonto("1", "999", BigDecimal.TEN))
                .isInstanceOf(ProductoNoDisponibleException.class);
        assertThatThrownBy(() -> service.validarProductoYMonto("5", "100", new BigDecimal("100")))
                .isInstanceOf(ProductoNoDisponibleException.class);
    }

    @Test
    void productoConPrecioIlegible_seDescarta() {
        assertThatThrownBy(() -> service.validarProductoYMonto("7", "700", BigDecimal.TEN))
                .isInstanceOf(ProductoNoDisponibleException.class);
    }

    @Test
    void catalogo_seReutilizaDuranteElTtlYSeRefrescaAlVencer() {
        service.validarProductoYMonto("1", "100", new BigDecimal("100"));
        service.validarProductoYMonto("1", "100", new BigDecimal("100"));
        verify(client, times(1)).getProductListRaw();

        clock.avanzar(Duration.ofMinutes(31));
        service.validarProductoYMonto("1", "100", new BigDecimal("100"));
        verify(client, times(2)).getProductListRaw();
    }

    @Test
    void proveedorCaidoOCatalogoVacio_fallaCerrado() {
        when(client.getProductListRaw()).thenThrow(new RuntimeException("caído"));
        assertThatThrownBy(() -> service.validarProductoYMonto("1", "100", new BigDecimal("100")))
                .isInstanceOf(CatalogoNoDisponibleException.class);

        doReturn("<RESPONSE><PRODUCTOS/></RESPONSE>").when(client).getProductListRaw();
        assertThatThrownBy(() -> service.validarProductoYMonto("1", "100", new BigDecimal("100")))
                .isInstanceOf(CatalogoNoDisponibleException.class);
    }

    static class MutableClock extends Clock {
        private Instant instante;

        MutableClock(Instant instante) {
            this.instante = instante;
        }

        void avanzar(Duration d) {
            instante = instante.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Presentación: servicio, descripción, categoría, logo y regla de referencia; orden por categoría")
    void listar_presentacionDelCatalogo() {
        when(client.getProductListRaw()).thenReturn("""
                <RESPONSE><PRODUCTOS>
                  <producto servicio="Telcel" producto="Telcel $50" idServicio="1" idProducto="50" precio="50"
                            idCatTipoServicio="1" tipoReferencia="NUMERO" longitudReferencia="10"/>
                  <producto servicio="CFE" producto="CFE" idServicio="5" idProducto="500" precio="0" categoria="Luz"
                            logo="ftp://no-seguro/logo.png" longitudMinima="12" longitudReferencia="30"/>
                  <producto producto="Sin servicio" idServicio="9" idProducto="900" precio="10" tipoServicio="7"/>
                  <producto servicio="Agua" producto="Agua" idServicio="8" idProducto="800" precio="0" leyenda="Pago de agua"/>
                </PRODUCTOS></RESPONSE>""");
        com.proyecto.servicios.config.GestoPagoCatalogoProperties presentacion = new com.proyecto.servicios.config.GestoPagoCatalogoProperties();
        presentacion.setLogoUrl("https://cdn.test/{idServicio}.png");
        presentacion.getCategorias().put("1", "Recargas");
        PagosProperties properties = new PagosProperties();
        properties.getCatalogo().setTtlMinutos(30);
        GestoPagoCatalogoProductosService conPresentacion = new GestoPagoCatalogoProductosService(client, properties, clock, presentacion);

        java.util.List<ProductoCatalogo> productos = conPresentacion.listar();

        assertThat(productos).extracting(ProductoCatalogo::categoria)
                .containsExactly("Categoría 7", "Luz", "Recargas", "Servicios");
        ProductoCatalogo telcel = conPresentacion.obtener("1", "50");
        assertThat(telcel.descripcion()).isEqualTo("Telcel - Telcel $50");
        assertThat(telcel.logoUrl()).isEqualTo("https://cdn.test/1.png");
        assertThat(telcel.referencia().soloDigitos()).isTrue();
        assertThat(telcel.referencia().longitudMaxima()).isEqualTo(10);
        ProductoCatalogo cfe = conPresentacion.obtener("5", "500");
        assertThat(cfe.descripcion()).isEqualTo("CFE");
        assertThat(cfe.logoUrl()).isEqualTo("https://cdn.test/5.png");
        assertThat(cfe.referencia().longitudMinima()).isEqualTo(12);
        assertThat(conPresentacion.obtener("9", "900").descripcion()).isEqualTo("Sin servicio");
        assertThat(conPresentacion.obtener("8", "800").descripcion()).isEqualTo("Pago de agua");
        // Sin plantilla de logo
        assertThat(service.listar()).allSatisfy(p -> assertThat(p.logoUrl()).isNull());
    }
}
