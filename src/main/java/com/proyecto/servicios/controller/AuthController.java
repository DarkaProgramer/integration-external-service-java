package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.auth.AlertaSesionDto;
import com.proyecto.servicios.model.auth.CerrarSesionRequestDto;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.PerfilDto;
import com.proyecto.servicios.model.auth.RefreshRequestDto;
import com.proyecto.servicios.model.auth.RegistroRequestDto;
import com.proyecto.servicios.model.auth.SesionDto;
import com.proyecto.servicios.model.auth.SesionTokensDto;
import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.seguridad.ClienteActual;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Autenticación compartida por la web y la app móvil.
 * <ul>
 *     <li>registro y login devuelven accessToken (corto) + refreshToken (rotativo) ligados a una sesión por dispositivo.</li>
 *     <li>El resto de endpoints requieren {@code Authorization: Bearer <accessToken>}.</li>
 *     <li>Alertas: cada dispositivo consulta {@code GET /api/auth/alertas} periódicamente (p. ej. cada 20 s) para
 *     enterarse de que la cuenta inició sesión en otro dispositivo.</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SesionTokensDto>> registrar(@Valid @RequestBody RegistroRequestDto solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "Usuario registrado", authService.registrar(solicitud)));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SesionTokensDto>> login(@Valid @RequestBody LoginRequestDto solicitud) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Sesión iniciada", authService.login(solicitud)));
    }

    @PostMapping(value = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<SesionTokensDto>> refrescar(@Valid @RequestBody RefreshRequestDto solicitud) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Sesión renovada", authService.refrescar(solicitud)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@ClienteActual ClienteAutenticado cliente) {
        authService.logout(cliente);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Sesión cerrada", null));
    }

    @GetMapping("/perfil")
    public ResponseEntity<ApiResponse<PerfilDto>> perfil(@ClienteActual ClienteAutenticado cliente) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Perfil del usuario", authService.perfil(cliente)));
    }

    @GetMapping("/sesiones")
    public ResponseEntity<ApiResponse<List<SesionDto>>> sesiones(@ClienteActual ClienteAutenticado cliente) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Sesiones activas", authService.sesiones(cliente)));
    }

    @PostMapping(value = "/sesiones/cerrar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> cerrarSesion(@ClienteActual ClienteAutenticado cliente,
                                                          @Valid @RequestBody CerrarSesionRequestDto solicitud) {
        authService.cerrarSesion(cliente, solicitud.getSesionId());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Sesión cerrada", null));
    }

    @GetMapping("/alertas")
    public ResponseEntity<ApiResponse<List<AlertaSesionDto>>> alertas(@ClienteActual ClienteAutenticado cliente) {
        List<AlertaSesionDto> alertas = authService.alertasPendientes(cliente);
        String mensaje = alertas.isEmpty() ? "Sin alertas" : "Se inició sesión en otro dispositivo";
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), mensaje, alertas));
    }

    @PostMapping("/alertas/leidas")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> marcarLeidas(@ClienteActual ClienteAutenticado cliente) {
        int marcadas = authService.marcarAlertasLeidas(cliente);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Alertas marcadas como leídas", Map.of("marcadas", marcadas)));
    }
}
