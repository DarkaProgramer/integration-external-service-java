package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.exception.pagos.AutenticacionClienteException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenClienteService - autenticación del cliente")
class TokenClienteServiceTest {

    private static final String SECRETO = "secreto-de-pruebas-de-al-menos-32-bytes!!";
    private static final Instant AHORA = Instant.parse("2026-10-29T10:00:00Z");

    private final TokenClienteService service =
            new TokenClienteService(SECRETO, "hasani-pagos", Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void tokenValido_devuelveClienteYPermisos() {
        String token = service.emitir(42L, List.of("pagos:servicios", "otro"), Duration.ofMinutes(5));

        ClienteAutenticado cliente = service.autenticar("Bearer " + token);

        assertThat(cliente.clienteId()).isEqualTo(42L);
        assertThat(cliente.tienePermiso("pagos:servicios")).isTrue();
        assertThat(cliente.tienePermiso("pagos:conciliar")).isFalse();
    }

    @Test
    void tokenExpirado_rechaza() {
        TokenClienteService emisorDelPasado = new TokenClienteService(SECRETO, "hasani-pagos",
                Clock.fixed(AHORA.minus(Duration.ofHours(1)), ZoneOffset.UTC));
        String token = emisorDelPasado.emitir(1L, List.of("pagos:servicios"), Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.autenticar("Bearer " + token)).isInstanceOf(AutenticacionClienteException.class);
    }

    @Test
    void tokenFirmadoConOtroSecreto_rechaza() {
        TokenClienteService otro = new TokenClienteService("otro-secreto-distinto-de-al-menos-32-bytes", "hasani-pagos",
                Clock.fixed(AHORA, ZoneOffset.UTC));
        String token = otro.emitir(1L, List.of("pagos:servicios"), Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.autenticar("Bearer " + token)).isInstanceOf(AutenticacionClienteException.class);
    }

    @Test
    void tokenDeOtroEmisorOSinExpiracion_rechaza() {
        var llave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        String otroEmisor = Jwts.builder().issuer("otro").subject("1").expiration(Date.from(AHORA.plusSeconds(60)))
                .signWith(llave).compact();
        String sinExpiracion = Jwts.builder().issuer("hasani-pagos").subject("1").signWith(llave).compact();

        assertThatThrownBy(() -> service.autenticar("Bearer " + otroEmisor)).isInstanceOf(AutenticacionClienteException.class);
        assertThatThrownBy(() -> service.autenticar("Bearer " + sinExpiracion)).isInstanceOf(AutenticacionClienteException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Bearer ", "Bearer no.es.jwt"})
    void encabezadoAusenteOMalFormado_rechaza(String encabezado) {
        assertThatThrownBy(() -> service.autenticar(encabezado)).isInstanceOf(AutenticacionClienteException.class);
    }

    @Test
    void sinSecretoConfigurado_rechazaTodo() {
        TokenClienteService sinSecreto = new TokenClienteService("corto", "hasani-pagos", Clock.systemUTC());
        String token = service.emitir(1L, List.of("pagos:servicios"), Duration.ofMinutes(5));

        assertThatThrownBy(() -> sinSecreto.autenticar("Bearer " + token)).isInstanceOf(AutenticacionClienteException.class);
    }
}
