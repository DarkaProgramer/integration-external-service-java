package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestoPagoBearerTokenTest {

    private static final long HORA = 3_600_000L;
    private final GestoPagoTokenService tokenService = mock(GestoPagoTokenService.class);

    private GestoPagoBearerToken conPassword(String password) {
        return new GestoPagoBearerToken(tokenService, "83", "DISP", password, HORA, "https://gp.test", "", 60);
    }

    private static Optional<GestoPagoToken> guardado(String valor, LocalDateTime actualizado) {
        GestoPagoToken t = new GestoPagoToken();
        t.setToken(valor);
        t.setFechaActualizacion(actualizado);
        return Optional.of(t);
    }

    @Test
    void obtener_tokenRenovadoReciente_tienePrioridadSobreElFijo() {
        when(tokenService.obtenerTokenActivo(83, "DISP")).thenReturn(guardado("Bearer nuevo", LocalDateTime.now()));

        assertThat(conPassword("clave").obtener("fijo-caducado")).isEqualTo("nuevo");
    }

    @Test
    void obtener_sinTokenGuardadoNiFijo_pideUnoNuevoConLaPassword() {
        when(tokenService.obtenerTokenActivo(83, "DISP"))
                .thenReturn(Optional.empty())
                .thenReturn(guardado("recien", LocalDateTime.now()));

        assertThat(conPassword("clave").obtener("")).isEqualTo("recien");
        verify(tokenService).renovarToken();
    }

    @Test
    void obtener_tokenGuardadoViejo_usaElFijo() {
        when(tokenService.obtenerTokenActivo(83, "DISP")).thenReturn(guardado("viejo", LocalDateTime.now().minusHours(5)));

        assertThat(conPassword("clave").obtener("fijo")).isEqualTo("fijo");
    }

    @Test
    void obtener_sinNada_lanzaErrorDeAutenticacion() {
        when(tokenService.obtenerTokenActivo(83, "DISP")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conPassword("").obtener(null))
                .isInstanceOfSatisfying(GestoPagoIntegrationException.class,
                        e -> assertThat(e.getErrorType()).isEqualTo(GestoPagoErrorType.AUTHENTICATION));
        verify(tokenService, never()).renovarToken();
    }

    @Test
    void obtener_gestoPagoNoRespondio_noReintentaEnLaSiguientePeticion() {
        when(tokenService.obtenerTokenActivo(83, "DISP")).thenReturn(Optional.empty());
        GestoPagoBearerToken token = conPassword("clave");

        assertThatThrownBy(() -> token.obtener("")).isInstanceOf(GestoPagoIntegrationException.class);
        assertThatThrownBy(() -> token.obtener("")).isInstanceOf(GestoPagoIntegrationException.class);

        // Solo el primer intento llama a Gestopago; el segundo responde de inmediato
        verify(tokenService, times(1)).renovarToken();
    }
}
