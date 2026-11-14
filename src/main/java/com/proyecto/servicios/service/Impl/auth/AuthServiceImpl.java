package com.proyecto.servicios.service.Impl.auth;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.seguridad.AlertaSesion;
import com.proyecto.servicios.entity.seguridad.CredencialCliente;
import com.proyecto.servicios.entity.seguridad.SesionCliente;
import com.proyecto.servicios.entity.seguridad.TipoDispositivo;
import com.proyecto.servicios.exception.auth.AccesoBloqueadoException;
import com.proyecto.servicios.exception.auth.CredencialesInvalidasException;
import com.proyecto.servicios.exception.auth.SesionInvalidaException;
import com.proyecto.servicios.exception.auth.SesionNoEncontradaException;
import com.proyecto.servicios.exception.auth.UsuarioYaRegistradoException;
import com.proyecto.servicios.exception.pagos.AutenticacionClienteException;
import com.proyecto.servicios.model.auth.AlertaSesionDto;
import com.proyecto.servicios.model.auth.DispositivoDto;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.PerfilDto;
import com.proyecto.servicios.model.auth.RefreshRequestDto;
import com.proyecto.servicios.model.auth.RegistroRequestDto;
import com.proyecto.servicios.model.auth.SesionDto;
import com.proyecto.servicios.model.auth.SesionTokensDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.AlertaSesionRepository;
import com.proyecto.servicios.repositorys.seguridad.CredencialClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.SesionClienteRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.seguridad.TokenClienteService;
import com.proyecto.servicios.seguridad.VerificadorSesion;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService, VerificadorSesion {

    static final String CIERRE_LOGOUT = "LOGOUT";
    static final String CIERRE_USUARIO = "CERRADA_POR_USUARIO";
    static final String CIERRE_CLIENTE_INACTIVO = "CLIENTE_INACTIVO";
    /** Para no escribir en la base en cada petición: el último uso se actualiza como mucho una vez por minuto. */
    private static final Duration PRECISION_ULTIMO_USO = Duration.ofMinutes(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteService clienteService;
    private final ClienteRepository clienteRepository;
    private final CredencialClienteRepository credencialRepository;
    private final SesionClienteRepository sesionRepository;
    private final AlertaSesionRepository alertaRepository;
    private final TokenClienteService tokenClienteService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Duration vigenciaAccess;
    private final Duration vigenciaRefresh;
    private final int maxIntentos;
    private final Duration duracionBloqueo;
    /** Hash de una contraseña cualquiera: si el correo no existe se compara igual, para que el tiempo de respuesta no lo delate. */
    private final String hashFicticio;

    @SuppressWarnings("java:S107") // dependencias + parámetros de configuración
    public AuthServiceImpl(ClienteService clienteService,
                           ClienteRepository clienteRepository,
                           CredencialClienteRepository credencialRepository,
                           SesionClienteRepository sesionRepository,
                           AlertaSesionRepository alertaRepository,
                           TokenClienteService tokenClienteService,
                           PasswordEncoder passwordEncoder,
                           Clock clock,
                           @Value("${app.seguridad.access-token-minutos:15}") long accessMinutos,
                           @Value("${app.seguridad.refresh-token-dias:30}") long refreshDias,
                           @Value("${app.seguridad.login.max-intentos:5}") int maxIntentos,
                           @Value("${app.seguridad.login.bloqueo-minutos:15}") long bloqueoMinutos) {
        this.clienteService = clienteService;
        this.clienteRepository = clienteRepository;
        this.credencialRepository = credencialRepository;
        this.sesionRepository = sesionRepository;
        this.alertaRepository = alertaRepository;
        this.tokenClienteService = tokenClienteService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.vigenciaAccess = Duration.ofMinutes(accessMinutos);
        this.vigenciaRefresh = Duration.ofDays(refreshDias);
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = Duration.ofMinutes(bloqueoMinutos);
        this.hashFicticio = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    // ───────────── Registro y login ─────────────

    @Override
    @Transactional
    public SesionTokensDto registrar(RegistroRequestDto solicitud) {
        String correo = normalizar(solicitud.getCorreo());
        if (credencialRepository.existsByCorreo(correo)) {
            throw new UsuarioYaRegistradoException();
        }
        // Mismas validaciones del onboarding (mayoría de edad, CURP/RFC/correo únicos); crea la cuenta con saldo inicial
        clienteService.registrar(solicitud);
        Cliente cliente = clienteRepository.findByCorreo(solicitud.getCorreo()).orElseThrow();

        CredencialCliente credencial = new CredencialCliente();
        credencial.setClienteId(cliente.getId());
        credencial.setCorreo(correo);
        credencial.setPasswordHash(passwordEncoder.encode(solicitud.getPassword()));
        credencial.setUltimoAcceso(ahora());
        credencialRepository.save(credencial);

        log.info("Usuario registrado: cliente={}", cliente.getId());
        return abrirSesion(cliente, solicitud.getDispositivo());
    }

    @Override
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, AccesoBloqueadoException.class})
    public SesionTokensDto login(LoginRequestDto solicitud) {
        LocalDateTime ahora = ahora();
        CredencialCliente credencial = credencialRepository.findByCorreo(normalizar(solicitud.getCorreo())).orElse(null);
        if (credencial == null) {
            passwordEncoder.matches(solicitud.getPassword(), hashFicticio);
            throw new CredencialesInvalidasException();
        }
        if (credencial.getBloqueadoHasta() != null && credencial.getBloqueadoHasta().isAfter(ahora)) {
            throw new AccesoBloqueadoException();
        }
        if (!passwordEncoder.matches(solicitud.getPassword(), credencial.getPasswordHash())) {
            registrarIntentoFallido(credencial, ahora);
            throw new CredencialesInvalidasException();
        }
        Cliente cliente = clienteRepository.findById(credencial.getClienteId())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(CredencialesInvalidasException::new);

        credencial.setIntentosFallidos(0);
        credencial.setBloqueadoHasta(null);
        credencial.setUltimoAcceso(ahora);
        credencialRepository.save(credencial);

        log.info("Login correcto: cliente={} dispositivo={}", cliente.getId(), solicitud.getDispositivo().getTipo());
        return abrirSesion(cliente, solicitud.getDispositivo());
    }

    private void registrarIntentoFallido(CredencialCliente credencial, LocalDateTime ahora) {
        int intentos = credencial.getIntentosFallidos() + 1;
        if (intentos >= maxIntentos) {
            credencial.setBloqueadoHasta(ahora.plus(duracionBloqueo));
            intentos = 0;
            log.warn("Acceso bloqueado temporalmente por intentos fallidos: cliente={}", credencial.getClienteId());
        }
        credencial.setIntentosFallidos(intentos);
        credencialRepository.save(credencial);
    }

    private SesionTokensDto abrirSesion(Cliente cliente, DispositivoDto dispositivo) {
        LocalDateTime ahora = ahora();
        List<SesionCliente> otras = sesionRepository.findActivas(cliente.getId(), ahora);

        SesionCliente sesion = new SesionCliente();
        sesion.setId(UUID.randomUUID().toString());
        sesion.setClienteId(cliente.getId());
        sesion.setTipoDispositivo(dispositivo.getTipo());
        sesion.setNombreDispositivo(nombreDispositivo(dispositivo));
        sesion.setCreatedAt(ahora);
        sesion.setUltimoUso(ahora);
        String refreshToken = nuevoRefreshToken();
        sesion.setRefreshHash(sha256(refreshToken));
        sesion.setRefreshExpira(ahora.plus(vigenciaRefresh));
        sesionRepository.save(sesion);

        // Aviso a cada dispositivo que ya tenía la sesión abierta
        alertaRepository.saveAll(otras.stream().map(destino -> alerta(destino, sesion, ahora)).toList());
        if (!otras.isEmpty()) {
            log.info("Nueva sesión con otras {} abiertas: cliente={} se enviaron alertas", otras.size(), cliente.getId());
        }

        return tokens(cliente, sesion, refreshToken)
                .otrasSesionesActivas(otras.stream().map(s -> aDto(s, sesion.getId())).toList())
                .build();
    }

    private AlertaSesion alerta(SesionCliente destino, SesionCliente nueva, LocalDateTime ahora) {
        AlertaSesion alerta = new AlertaSesion();
        alerta.setClienteId(nueva.getClienteId());
        alerta.setSesionDestinoId(destino.getId());
        alerta.setSesionOrigenId(nueva.getId());
        alerta.setTipoDispositivo(nueva.getTipoDispositivo());
        alerta.setNombreDispositivo(nueva.getNombreDispositivo());
        alerta.setFecha(ahora);
        return alerta;
    }

    // ───────────── Refresh y logout ─────────────

    @Override
    @Transactional
    public SesionTokensDto refrescar(RefreshRequestDto solicitud) {
        LocalDateTime ahora = ahora();
        SesionCliente sesion = sesionRepository.findByRefreshHash(sha256(solicitud.getRefreshToken()))
                .filter(s -> s.activaEn(ahora))
                .orElseThrow(SesionInvalidaException::new);
        Cliente cliente = clienteRepository.findById(sesion.getClienteId())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElse(null);
        if (cliente == null) {
            cerrar(sesion, CIERRE_CLIENTE_INACTIVO, ahora);
            throw new SesionInvalidaException();
        }
        // Rotación: el refresh token anterior deja de servir
        String refreshToken = nuevoRefreshToken();
        sesion.setRefreshHash(sha256(refreshToken));
        sesion.setRefreshExpira(ahora.plus(vigenciaRefresh));
        sesion.setUltimoUso(ahora);
        sesionRepository.save(sesion);
        return tokens(cliente, sesion, refreshToken).build();
    }

    @Override
    @Transactional
    public void logout(ClienteAutenticado cliente) {
        if (cliente.sesionId() == null) {
            return;
        }
        sesionRepository.findById(cliente.sesionId())
                .filter(s -> s.getClienteId().equals(cliente.clienteId()) && s.getCerradaEn() == null)
                .ifPresent(s -> cerrar(s, CIERRE_LOGOUT, ahora()));
    }

    @Override
    @Transactional
    public void cerrarSesion(ClienteAutenticado cliente, String sesionId) {
        SesionCliente sesion = sesionRepository.findById(sesionId.toLowerCase(Locale.ROOT))
                .filter(s -> s.getClienteId().equals(cliente.clienteId()))
                .orElseThrow(SesionNoEncontradaException::new);
        if (sesion.getCerradaEn() == null) {
            cerrar(sesion, CIERRE_USUARIO, ahora());
        }
    }

    private void cerrar(SesionCliente sesion, String motivo, LocalDateTime ahora) {
        sesion.setCerradaEn(ahora);
        sesion.setMotivoCierre(motivo);
        sesionRepository.save(sesion);
        log.info("Sesión cerrada: cliente={} motivo={}", sesion.getClienteId(), motivo);
    }

    @Override
    @Transactional
    public void verificarActiva(ClienteAutenticado cliente) {
        LocalDateTime ahora = ahora();
        SesionCliente sesion = sesionRepository.findById(cliente.sesionId())
                .filter(s -> s.getClienteId().equals(cliente.clienteId()) && s.activaEn(ahora))
                .orElseThrow(AutenticacionClienteException::new);
        if (sesion.getUltimoUso().plus(PRECISION_ULTIMO_USO).isBefore(ahora)) {
            sesion.setUltimoUso(ahora);
            sesionRepository.save(sesion);
        }
    }

    // ───────────── Consultas ─────────────

    @Override
    @Transactional(readOnly = true)
    public PerfilDto perfil(ClienteAutenticado cliente) {
        return clienteRepository.findById(cliente.clienteId())
                .map(this::perfil)
                .orElseThrow(AutenticacionClienteException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SesionDto> sesiones(ClienteAutenticado cliente) {
        return sesionRepository.findActivas(cliente.clienteId(), ahora()).stream()
                .map(s -> aDto(s, cliente.sesionId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaSesionDto> alertasPendientes(ClienteAutenticado cliente) {
        if (cliente.sesionId() == null) {
            return List.of();
        }
        return alertaRepository.findBySesionDestinoIdAndLeidaFalseOrderByFechaDesc(cliente.sesionId()).stream()
                .map(a -> AlertaSesionDto.builder()
                        .id(a.getId())
                        .mensaje(mensajeAlerta(a.getTipoDispositivo(), a.getNombreDispositivo()))
                        .sesionId(a.getSesionOrigenId())
                        .tipoDispositivo(a.getTipoDispositivo())
                        .nombreDispositivo(a.getNombreDispositivo())
                        .fecha(a.getFecha())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public int marcarAlertasLeidas(ClienteAutenticado cliente) {
        return cliente.sesionId() == null ? 0 : alertaRepository.marcarLeidas(cliente.sesionId());
    }

    // ───────────── Utilidades ─────────────

    private SesionTokensDto.SesionTokensDtoBuilder tokens(Cliente cliente, SesionCliente sesion, String refreshToken) {
        String accessToken = tokenClienteService.emitir(cliente.getId(),
                List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), vigenciaAccess, sesion.getId());
        return SesionTokensDto.builder()
                .accessToken(accessToken)
                .expiraEn(vigenciaAccess.toSeconds())
                .refreshToken(refreshToken)
                .refreshExpiraEn(Duration.between(ahora(), sesion.getRefreshExpira()).toSeconds())
                .sesionId(sesion.getId())
                .perfil(perfil(cliente));
    }

    private PerfilDto perfil(Cliente cliente) {
        Cuenta cuenta = cliente.getCuenta();
        return PerfilDto.builder()
                .nombre(cliente.getNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .numeroCuenta(cuenta == null ? null : cuenta.getNumeroCuenta())
                .saldo(cuenta == null ? null : cuenta.getSaldo())
                .estatusCuenta(cuenta == null ? null : cuenta.getEstatus())
                .build();
    }

    private SesionDto aDto(SesionCliente s, String sesionActual) {
        return SesionDto.builder()
                .sesionId(s.getId())
                .tipoDispositivo(s.getTipoDispositivo())
                .nombreDispositivo(s.getNombreDispositivo())
                .inicio(s.getCreatedAt())
                .ultimoUso(s.getUltimoUso())
                .actual(s.getId().equals(sesionActual))
                .build();
    }

    static String mensajeAlerta(TipoDispositivo tipo, String nombre) {
        String dispositivo = tipo == TipoDispositivo.MOVIL ? "la app móvil" : "la web";
        String detalle = nombre == null ? "" : " (" + nombre + ")";
        return "Se inició sesión en tu cuenta desde " + dispositivo + detalle
                + ". Si no fuiste tú, cierra esa sesión y cambia tu contraseña";
    }

    private static String nombreDispositivo(DispositivoDto dispositivo) {
        String nombre = dispositivo.getNombre();
        return nombre == null || nombre.isBlank() ? null : nombre.strip();
    }

    static String normalizar(String correo) {
        return correo.strip().toLowerCase(Locale.ROOT);
    }

    private static String nuevoRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }
}
