package com.proyecto.servicios.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GestoPagoCredencialesTest {

    @Test
    void idDistribuidor_vacioCeroOInvalido_usa83() {
        assertThat(GestoPagoCredenciales.idDistribuidor(null)).isEqualTo(83);
        assertThat(GestoPagoCredenciales.idDistribuidor("")).isEqualTo(83);
        assertThat(GestoPagoCredenciales.idDistribuidor("0")).isEqualTo(83);
        assertThat(GestoPagoCredenciales.idDistribuidor("abc")).isEqualTo(83);
        assertThat(GestoPagoCredenciales.idDistribuidor(" 120 ")).isEqualTo(120);
    }

    @Test
    void codigoDispositivo_vacio_usaElDelProyecto() {
        assertThat(GestoPagoCredenciales.codigoDispositivo(" ")).isEqualTo("GPS83-TPV-17");
        assertThat(GestoPagoCredenciales.codigoDispositivo("OTRO")).isEqualTo("OTRO");
    }
}
