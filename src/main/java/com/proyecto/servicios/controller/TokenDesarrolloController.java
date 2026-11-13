package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.seguridad.TokenClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * SOLO PARA DESARROLLO Y DEMOSTRACIÓN (perfil "dev"): emite un token de cliente de 15 minutos.
 * En producción el token lo emite el servicio de login/autenticación del banco; este endpoint no existe
 * si no se activa explícitamente el perfil dev.
 */
@Profile("dev")
@RestController
@RequiredArgsConstructor
public class TokenDesarrolloController {

    private final TokenClienteService tokenClienteService;

    @PostMapping(value = "/api/dev/token", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> emitir(@Valid @RequestBody SolicitudToken solicitud) {
        String token = tokenClienteService.emitir(solicitud.getClienteId(),
                List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(15));
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Token de desarrollo emitido",
                Map.of("tokenType", "Bearer", "accessToken", token)));
    }

    @Data
    public static class SolicitudToken {
        @NotNull(message = "El clienteId es obligatorio")
        @Positive(message = "El clienteId debe ser positivo")
        private Long clienteId;
    }
}
