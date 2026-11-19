package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Con gestopago.modo=simulado la aplicación usa el catálogo, la validación y los pagos de práctica. */
@SpringBootTest(properties = "gestopago.modo=simulado")
@ActiveProfiles("test")
class ModoSimuladoContextTest {

    @Autowired
    private CatalogoProductosService catalogo;
    @Autowired
    private ValidadorReferenciaGateway referencias;
    @Autowired
    private ProveedorPagosGateway pagos;

    @Test
    void usaLosAdaptadoresSimulados() {
        assertThat(catalogo).isInstanceOf(CatalogoSimulado.class);
        assertThat(referencias).isInstanceOf(ReferenciasSimuladas.class);
        assertThat(pagos).isInstanceOf(PagosSimulados.class);
        assertThat(catalogo.listar()).isNotEmpty();
    }
}
