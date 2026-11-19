package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.exception.pagos.TarjetaInvalidaException;
import com.proyecto.servicios.exception.pagos.TarjetaRechazadaException;
import com.proyecto.servicios.model.pagos.TarjetaDto;
import com.proyecto.servicios.service.pagos.AutorizacionTarjeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcesadorTarjetasSimuladoTest {

    private final ProcesadorTarjetasSimulado procesador =
            new ProcesadorTarjetasSimulado(Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC));

    static TarjetaDto tarjeta(String numero, int mes, int anio, String cvv) {
        TarjetaDto t = new TarjetaDto();
        t.setNumero(numero);
        t.setTitular("Angel Perez Lopez");
        t.setMesVencimiento(mes);
        t.setAnioVencimiento(anio);
        t.setCvv(cvv);
        return t;
    }

    @ParameterizedTest
    @ValueSource(strings = {"4000 1234 5675 9010", "5200123456754321"})
    void debitoValida_seAutoriza(String numero) {
        AutorizacionTarjeta a = procesador.autorizar(tarjeta(numero, 12, 2028, "123"), BigDecimal.TEN);

        assertThat(a.ultimos4()).isEqualTo(numero.replace(" ", "").substring(12));
        assertThat(a.codigo()).matches("\\d{6}");
        assertThat(a.marca()).isIn("VISA", "MASTERCARD");
    }

    @Test
    void credito_seRechaza() {
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4111111111111111", 12, 2028, "123"), BigDecimal.TEN))
                .isInstanceOf(TarjetaInvalidaException.class).hasMessageContaining("crédito");
    }

    @Test
    void amex_noSeAcepta() {
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("378282246310005", 12, 2028, "1234"), BigDecimal.TEN))
                .isInstanceOf(TarjetaInvalidaException.class).hasMessageContaining("débito");
    }

    @Test
    void luhnInvalido_vencidaYCvv() {
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4000123456759011", 12, 2028, "123"), BigDecimal.TEN))
                .isInstanceOf(TarjetaInvalidaException.class).hasMessageContaining("no es válido");
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4000123456759010", 10, 2026, "123"), BigDecimal.TEN))
                .isInstanceOf(TarjetaInvalidaException.class).hasMessageContaining("vencida");
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4000123456759010", 12, 2028, "12"), BigDecimal.TEN))
                .isInstanceOf(TarjetaInvalidaException.class).hasMessageContaining("CVV");
    }

    @Test
    void rechazosDelBanco() {
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4000123456740002", 12, 2028, "123"), BigDecimal.TEN))
                .isInstanceOf(TarjetaRechazadaException.class).hasMessageContaining("fondos insuficientes");
        assertThatThrownBy(() -> procesador.autorizar(tarjeta("4000123456740069", 12, 2028, "123"), BigDecimal.TEN))
                .isInstanceOf(TarjetaRechazadaException.class).hasMessageContaining("bloqueada");
    }
}
