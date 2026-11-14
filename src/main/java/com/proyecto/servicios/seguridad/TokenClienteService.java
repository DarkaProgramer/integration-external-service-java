package com.proyecto.servicios.seguridad;

import com.proyecto.servicios.exception.pagos.AutenticacionClienteException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Valida (y, para pruebas/desarrollo, emite) los JWT con los que el cliente se identifica ante este API.
 * <ul>
 *     <li>Firma HMAC-SHA256 con un secreto de al menos 32 bytes que llega por variable de entorno (APP_JWT_SECRET).</li>
 *     <li>Se exige emisor, sujeto numérico (id del cliente) y expiración.</li>
 *     <li>Los permisos van en el claim {@code scope} separados por espacio (convención OAuth 2).</li>
 * </ul>
 * Este token es distinto del token Bearer de Gestopago, que nunca sale del servidor.
 */
@Slf4j
@Service
public class TokenClienteService {

    static final String CLAIM_SCOPE = "scope";
    static final String CLAIM_SESION = "sid";
    private static final String PREFIJO_BEARER = "Bearer ";
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final SecretKey llave;
    private final String emisor;
    private final Clock clock;

    public TokenClienteService(@Value("${app.seguridad.jwt.secreto:}") String secreto,
                               @Value("${app.seguridad.jwt.emisor:hasani-pagos}") String emisor,
                               Clock clock) {
        this.emisor = emisor;
        this.clock = clock;
        if (StringUtils.isBlank(secreto) || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            // No se detiene el arranque (el resto del API no lo necesita), pero ninguna operación de pago se autentica.
            log.error("app.seguridad.jwt.secreto no está configurado o tiene menos de {} bytes: se rechazarán todas las operaciones de pago",
                    LONGITUD_MINIMA_SECRETO);
            this.llave = null;
        } else {
            this.llave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        }
    }

    public ClienteAutenticado autenticar(String encabezadoAuthorization) {
        if (llave == null || encabezadoAuthorization == null || !encabezadoAuthorization.startsWith(PREFIJO_BEARER)) {
            throw new AutenticacionClienteException();
        }
        String token = encabezadoAuthorization.substring(PREFIJO_BEARER.length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(llave)
                    .requireIssuer(emisor)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (claims.getExpiration() == null || !StringUtils.isNumeric(claims.getSubject())) {
                throw new AutenticacionClienteException();
            }
            Object sesion = claims.get(CLAIM_SESION);
            return new ClienteAutenticado(Long.valueOf(claims.getSubject()), permisos(claims.get(CLAIM_SCOPE)),
                    sesion == null ? null : String.valueOf(sesion));
        } catch (JwtException | IllegalArgumentException e) {
            // Solo el tipo: el mensaje de jjwt puede incluir partes del token
            log.warn("Token de cliente rechazado: {}", e.getClass().getSimpleName());
            throw new AutenticacionClienteException();
        }
    }

    public String emitir(Long clienteId, Collection<String> permisos, Duration vigencia) {
        return emitir(clienteId, permisos, vigencia, null);
    }

    /** Emite un token ligado a una sesión: al cerrar la sesión el token deja de ser aceptado aunque no haya expirado. */
    public String emitir(Long clienteId, Collection<String> permisos, Duration vigencia, String sesionId) {
        if (llave == null) {
            throw new IllegalStateException("No hay secreto JWT configurado");
        }
        Instant ahora = clock.instant();
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(emisor)
                .subject(String.valueOf(clienteId))
                .claim(CLAIM_SCOPE, String.join(" ", permisos))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)));
        if (sesionId != null) {
            builder.claim(CLAIM_SESION, sesionId);
        }
        return builder.signWith(llave).compact();
    }

    private Set<String> permisos(Object scope) {
        if (scope instanceof String texto) {
            return Arrays.stream(texto.trim().split("\\s+")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        }
        if (scope instanceof Collection<?> lista) {
            return lista.stream().map(String::valueOf).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return Set.of();
    }
}
