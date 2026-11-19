package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.GestoPagoErrorType;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Token Bearer para los servicios de Gestopago (catálogo, transacciones y referencias).
 * <ol>
 *     <li>Primero el token que la app obtiene sola con {@code GESTOPAGO_PASSWORD} en
 *     {@code /sistema/app/jwt-gp/authenticate/} y renueva cada hora ({@link GestoPagoTokenService}).</li>
 *     <li>Si no hay uno reciente, el token fijo de la variable de entorno ({@code GESTOPAGO_PRODUCTS_TOKEN} /
 *     {@code GESTOPAGO_TX_TOKEN}).</li>
 * </ol>
 * Antes solo se usaba el token fijo: al caducar (los JWT de Gestopago duran poco) el catálogo dejaba de cargar
 * aunque la contraseña estuviera bien configurada.
 * <p>
 * Si Gestopago no responde al pedir el token, durante {@code gestopago.auth.espera-tras-fallo-segundos} las
 * peticiones de los usuarios ya no lo vuelven a pedir: responden de inmediato con error en lugar de quedarse
 * esperando (la renovación programada sigue intentando en segundo plano).
 */
@Slf4j
@Component
public class GestoPagoBearerToken {

    private final GestoPagoTokenService tokenService;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;
    private final String password;
    private final Duration vigencia;
    private final Duration esperaTrasFallo;
    private final AtomicReference<Instant> ultimoFallo = new AtomicReference<>();

    public GestoPagoBearerToken(GestoPagoTokenService tokenService,
                                @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
                                @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo,
                                @Value("${gestopago.auth.password:}") String password,
                                @Value("${gestopago.auth.refresh-rate-ms:3600000}") long refrescoMs,
                                @Value("${gestopago.base-url:}") String urlBase,
                                @Value("${gestopago.products.bearer-token:}") String tokenProductos,
                                @Value("${gestopago.auth.espera-tras-fallo-segundos:60}") long esperaTrasFalloSegundos) {
        this.tokenService = tokenService;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
        this.password = password;
        // Se considera vigente hasta dos ciclos de renovación (si una renovación falla, aún hay margen)
        this.vigencia = Duration.ofMillis(refrescoMs * 2);
        this.esperaTrasFallo = Duration.ofSeconds(Math.max(0, esperaTrasFalloSegundos));
        // Resumen al arrancar (sin secretos) para revisar en los logs de Render qué variables llegaron
        log.info("Configuración GestoPago: url={}, idDistribuidor={}, GESTOPAGO_PASSWORD={}, GESTOPAGO_PRODUCTS_TOKEN={}",
                urlBase, idDistribuidor, StringUtils.isNotBlank(password) ? "configurada" : "FALTA",
                StringUtils.isNotBlank(tokenProductos) ? "configurado" : "no");
    }

    /**
     * @param tokenFijo token de la variable de entorno (puede venir vacío)
     * @return el token a enviar en {@code Authorization: Bearer ...}
     * @throws GestoPagoIntegrationException (AUTHENTICATION) si no hay ningún token disponible
     */
    public String obtener(String tokenFijo) {
        Optional<String> dinamico = tokenGuardado(true);
        if (dinamico.isEmpty() && StringUtils.isNotBlank(password) && StringUtils.isBlank(tokenFijo)
                && !enEsperaTrasFallo()) {
            // Primer uso (o la renovación programada aún no corre): pedir uno en este momento
            renovar();
            dinamico = tokenGuardado(true);
            if (dinamico.isEmpty()) {
                ultimoFallo.set(Instant.now());
            }
        }
        if (dinamico.isPresent()) {
            return dinamico.get();
        }
        if (StringUtils.isNotBlank(tokenFijo)) {
            return tokenFijo.trim();
        }
        Optional<String> viejo = tokenGuardado(false);
        if (viejo.isPresent()) {
            log.warn("Se usa un token de GestoPago sin renovar recientemente; revisar GESTOPAGO_PASSWORD");
            return viejo.get();
        }
        log.error("Sin token de GestoPago: configurar GESTOPAGO_PASSWORD (recomendado) o GESTOPAGO_PRODUCTS_TOKEN");
        throw new GestoPagoIntegrationException(GestoPagoErrorType.AUTHENTICATION);
    }

    /** Pide un token nuevo a Gestopago (p. ej. después de un 401). No hace nada sin contraseña configurada. */
    public void renovar() {
        if (StringUtils.isBlank(password)) {
            log.error("GESTOPAGO_PASSWORD no está configurada: no se puede obtener un token nuevo de GestoPago");
            return;
        }
        tokenService.renovarToken();
    }

    private boolean enEsperaTrasFallo() {
        Instant fallo = ultimoFallo.get();
        boolean esperando = fallo != null && fallo.plus(esperaTrasFallo).isAfter(Instant.now());
        if (esperando) {
            log.warn("GestoPago no entregó token hace menos de {} s: no se reintenta en esta petición",
                    esperaTrasFallo.toSeconds());
        }
        return esperando;
    }

    private Optional<String> tokenGuardado(boolean soloReciente) {
        try {
            return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .filter(t -> StringUtils.isNotBlank(t.getToken()))
                    .filter(t -> !soloReciente || esReciente(t))
                    .map(t -> StringUtils.removeStartIgnoreCase(t.getToken().trim(), "Bearer ").trim());
        } catch (RuntimeException e) {
            log.warn("No se pudo leer el token guardado de GestoPago: causa={}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private boolean esReciente(GestoPagoToken token) {
        LocalDateTime actualizado = token.getFechaActualizacion();
        return actualizado != null && actualizado.isAfter(LocalDateTime.now().minus(vigencia));
    }
}
