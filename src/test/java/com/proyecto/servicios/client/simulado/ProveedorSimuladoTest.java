package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.exception.pagos.MontoNoPermitidoException;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProveedorSimuladoTest {

    private final CatalogoSimulado catalogo = new CatalogoSimulado(new PagosProperties());

    @Test
    void catalogo_tieneVariasCategorias() {
        assertThat(catalogo.listar()).hasSizeGreaterThan(20);
        assertThat(catalogo.listar().stream().map(ProductoCatalogo::categoria).distinct())
                .contains("Recargas", "Luz", "Agua", "Internet y telefonía");
    }

    @Test
    void validarMonto_precioFijoDistinto_lanzaError() {
        assertThatThrownBy(() -> catalogo.validarProductoYMonto("101", "100", new BigDecimal("50")))
                .isInstanceOf(MontoNoPermitidoException.class);
        assertThat(catalogo.validarProductoYMonto("101", "100", new BigDecimal("100")).nombre()).isEqualTo("Telcel $100");
    }

    @Test
    void referenciasYPagos_casosDePrueba() {
        ProductoCatalogo cfe = catalogo.obtener("201", "1");
        ReferenciasSimuladas referencias = new ReferenciasSimuladas();
        assertThat(referencias.validar(cfe, "123456789012").estado()).isEqualTo(ResultadoValidacionReferencia.Estado.VALIDA);
        // Recibo: adeudo fijo por referencia (se paga completo). Recarga de precio fijo: sin adeudo
        BigDecimal adeudo = referencias.validar(cfe, "123456789012").montoAdeudo();
        assertThat(adeudo).isBetween(new BigDecimal("150.00"), new BigDecimal("2499.99"));
        assertThat(referencias.validar(cfe, "123456789012").montoAdeudo()).isEqualTo(adeudo);
        assertThat(referencias.validar(catalogo.obtener("101", "100"), "5512345678").montoAdeudo()).isNull();
        assertThat(referencias.validar(cfe, "000012345678").estado()).isEqualTo(ResultadoValidacionReferencia.Estado.INVALIDA);

        PagosSimulados pagos = new PagosSimulados();
        assertThat(pagos.enviarPago(new SolicitudPagoProveedor("F1", "201", "1", "123456781234", BigDecimal.TEN)).estado())
                .isEqualTo(ResultadoProveedor.Estado.APROBADA);
        assertThat(pagos.enviarPago(new SolicitudPagoProveedor("F2", "201", "1", "123456789999", BigDecimal.TEN)).estado())
                .isEqualTo(ResultadoProveedor.Estado.RECHAZADA);
    }
}
