package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.model.pagos.ResultadoPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReglaReferencia y ResultadoPago")
class ReglaReferenciaTest {

    @Test
    void reglaGeneral() {
        ReglaReferencia r = ReglaReferencia.GENERAL;
        assertThat(r.errores(null)).contains("La referencia es obligatoria");
        assertThat(r.errores(" ")).isPresent();
        assertThat(r.errores("55-12")).contains("La referencia solo admite letras y dígitos, sin espacios ni símbolos");
        assertThat(r.errores("abc")).contains("La referencia debe tener entre 4 y 40 caracteres");
        assertThat(r.errores("ABC123")).isEmpty();
    }

    @Test
    void reglaNumericaYLimites() {
        ReglaReferencia r = new ReglaReferencia(10, 10, true);
        assertThat(r.errores("55123A5678")).contains("La referencia de este servicio solo admite dígitos");
        assertThat(r.errores("55123")).contains("La referencia debe tener exactamente 10 caracteres");
        assertThat(r.errores("5512345678")).isEmpty();
        ReglaReferencia ajustada = new ReglaReferencia(0, 99, false);
        assertThat(ajustada.longitudMinima()).isEqualTo(1);
        assertThat(ajustada.longitudMaxima()).isEqualTo(40);
        assertThat(new ReglaReferencia(8, 3, false).longitudMaxima()).isEqualTo(8);
    }

    @Test
    void resultadoPago() {
        assertThat(ResultadoPago.de(EstatusPago.APROBADA)).isEqualTo(ResultadoPago.EXITOSO);
        assertThat(ResultadoPago.de(EstatusPago.RECHAZADA)).isEqualTo(ResultadoPago.RECHAZADO);
        assertThat(ResultadoPago.de(EstatusPago.EN_CONCILIACION)).isEqualTo(ResultadoPago.PENDIENTE);
        assertThat(ResultadoPago.PENDIENTE.estatus()).contains(EstatusPago.REVISION_MANUAL, EstatusPago.PENDIENTE);
        assertThat(ResultadoPago.mensaje(EstatusPago.REVISION_MANUAL)).contains("revisión");
        assertThat(new ProductoCatalogo("1", "2", "x", null).identificador()).isEqualTo("1-2");
    }
}
