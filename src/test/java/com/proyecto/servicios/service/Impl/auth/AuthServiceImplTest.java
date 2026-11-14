package com.proyecto.servicios.service.Impl.auth;

import com.proyecto.servicios.entity.client.Cliente;
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
import com.proyecto.servicios.model.auth.DispositivoDto;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.RefreshRequestDto;
import com.proyecto.servicios.model.auth.RegistroRequestDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.AlertaSesionRepository;
import com.proyecto.servicios.repositorys.seguridad.CredencialClienteRepository;
import com.proyecto.servicios.repositorys.seguridad.SesionClienteRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.seguridad.TokenClienteService;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime AHORA = LocalDateTime.now(CLOCK);
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock private ClienteService clienteService;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CredencialClienteRepository credencialRepository;
    @Mock private SesionClienteRepository sesionRepository;
    @Mock private AlertaSesionRepository alertaRepository;

    private AuthServiceImpl service;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        TokenClienteService tokens = new TokenClienteService("secreto-de-pruebas-de-al-menos-32-bytes!!", "hasani-pagos", CLOCK);
        service = new AuthServiceImpl(clienteService, clienteRepository, credencialRepository, sesionRepository,
                alertaRepository, tokens, ENCODER, CLOCK, 15, 30, 3, 15);
        cliente = new Cliente();
        cliente.setId(7L);
        cliente.setNombre("Ana");
        cliente.setCorreo("ana@test.com");
        cliente.setActivo(true);
        lenient().when(sesionRepository.findActivas(any(), any())).thenReturn(List.of());
    }

    private CredencialCliente credencial(String password) {
        CredencialCliente c = new CredencialCliente();
        c.setClienteId(7L);
        c.setCorreo("ana@test.com");
        c.setPasswordHash(ENCODER.encode(password));
        return c;
    }

    private LoginRequestDto login(String correo, String password) {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setCorreo(correo);
        dto.setPassword(password);
        dto.setDispositivo(new DispositivoDto(TipoDispositivo.WEB, "  Chrome  "));
        return dto;
    }

    private SesionCliente sesion(String id, LocalDateTime cerrada) {
        SesionCliente s = new SesionCliente();
        s.setId(id);
        s.setClienteId(7L);
        s.setTipoDispositivo(TipoDispositivo.MOVIL);
        s.setRefreshExpira(AHORA.plusDays(1));
        s.setUltimoUso(AHORA.minusMinutes(5));
        s.setCreatedAt(AHORA.minusDays(1));
        s.setCerradaEn(cerrada);
        return s;
    }

    @Test
    @DisplayName("Login correcto: normaliza el correo, reinicia intentos y alerta a las otras sesiones")
    void loginCorrecto() {
        CredencialCliente credencial = credencial("Secreta123");
        credencial.setIntentosFallidos(2);
        when(credencialRepository.findByCorreo("ana@test.com")).thenReturn(Optional.of(credencial));
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        when(sesionRepository.findActivas(eq(7L), any())).thenReturn(List.of(sesion("s-movil", null)));

        var tokens = service.login(login("  ANA@test.com ", "Secreta123"));

        assertThat(tokens.getAccessToken()).isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotBlank();
        assertThat(tokens.getExpiraEn()).isEqualTo(900);
        assertThat(tokens.getOtrasSesionesActivas()).singleElement().satisfies(s -> assertThat(s.isActual()).isFalse());
        assertThat(credencial.getIntentosFallidos()).isZero();
        ArgumentCaptor<SesionCliente> nueva = ArgumentCaptor.forClass(SesionCliente.class);
        verify(sesionRepository).save(nueva.capture());
        assertThat(nueva.getValue().getNombreDispositivo()).isEqualTo("Chrome");
        assertThat(nueva.getValue().getRefreshHash()).isEqualTo(AuthServiceImpl.sha256(tokens.getRefreshToken()));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AlertaSesion>> alertas = ArgumentCaptor.forClass(List.class);
        verify(alertaRepository).saveAll(alertas.capture());
        assertThat(alertas.getValue()).singleElement().satisfies(a -> {
            assertThat(a.getSesionDestinoId()).isEqualTo("s-movil");
            assertThat(a.getTipoDispositivo()).isEqualTo(TipoDispositivo.WEB);
        });
    }

    @Test
    @DisplayName("Correo inexistente o cliente dado de baja: 401 genérico")
    void loginSinUsuarioOClienteInactivo() {
        when(credencialRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(login("x@test.com", "Secreta123"))).isInstanceOf(CredencialesInvalidasException.class);

        when(credencialRepository.findByCorreo("ana@test.com")).thenReturn(Optional.of(credencial("Secreta123")));
        cliente.setActivo(false);
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        assertThatThrownBy(() -> service.login(login("ana@test.com", "Secreta123"))).isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("Contraseña incorrecta suma intentos y al llegar al máximo bloquea")
    void bloqueo() {
        CredencialCliente credencial = credencial("Secreta123");
        when(credencialRepository.findByCorreo("ana@test.com")).thenReturn(Optional.of(credencial));

        assertThatThrownBy(() -> service.login(login("ana@test.com", "Mala1234"))).isInstanceOf(CredencialesInvalidasException.class);
        assertThat(credencial.getIntentosFallidos()).isEqualTo(1);
        assertThatThrownBy(() -> service.login(login("ana@test.com", "Mala1234"))).isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(() -> service.login(login("ana@test.com", "Mala1234"))).isInstanceOf(CredencialesInvalidasException.class);
        assertThat(credencial.getBloqueadoHasta()).isEqualTo(AHORA.plusMinutes(15));
        assertThat(credencial.getIntentosFallidos()).isZero();

        assertThatThrownBy(() -> service.login(login("ana@test.com", "Secreta123"))).isInstanceOf(AccesoBloqueadoException.class);
        verify(clienteRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Registro: correo ya con credenciales 409; si no, crea cliente, credencial con hash y sesión")
    void registro() {
        RegistroRequestDto dto = new RegistroRequestDto();
        dto.setCorreo("Ana@Test.com");
        dto.setPassword("Secreta123");
        dto.setDispositivo(new DispositivoDto(TipoDispositivo.MOVIL, null));

        when(credencialRepository.existsByCorreo("ana@test.com")).thenReturn(true);
        assertThatThrownBy(() -> service.registrar(dto)).isInstanceOf(UsuarioYaRegistradoException.class);

        when(credencialRepository.existsByCorreo("ana@test.com")).thenReturn(false);
        when(clienteRepository.findByCorreo("Ana@Test.com")).thenReturn(Optional.of(cliente));
        var tokens = service.registrar(dto);

        verify(clienteService).registrar(dto);
        ArgumentCaptor<CredencialCliente> credencial = ArgumentCaptor.forClass(CredencialCliente.class);
        verify(credencialRepository).save(credencial.capture());
        assertThat(credencial.getValue().getCorreo()).isEqualTo("ana@test.com");
        assertThat(credencial.getValue().getPasswordHash()).isNotEqualTo("Secreta123");
        assertThat(ENCODER.matches("Secreta123", credencial.getValue().getPasswordHash())).isTrue();
        assertThat(tokens.getPerfil().getCorreo()).isEqualTo("ana@test.com");
        assertThat(tokens.getOtrasSesionesActivas()).isEmpty();
    }

    @Test
    @DisplayName("Refresh: desconocido, cerrado o de cliente inactivo es 401; el válido se rota")
    void refresh() {
        when(sesionRepository.findByRefreshHash(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.refrescar(new RefreshRequestDto("x"))).isInstanceOf(SesionInvalidaException.class);

        SesionCliente cerrada = sesion("s1", AHORA.minusMinutes(1));
        when(sesionRepository.findByRefreshHash(AuthServiceImpl.sha256("cerrada"))).thenReturn(Optional.of(cerrada));
        assertThatThrownBy(() -> service.refrescar(new RefreshRequestDto("cerrada"))).isInstanceOf(SesionInvalidaException.class);

        SesionCliente activa = sesion("s2", null);
        when(sesionRepository.findByRefreshHash(AuthServiceImpl.sha256("bueno"))).thenReturn(Optional.of(activa));
        cliente.setActivo(false);
        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        assertThatThrownBy(() -> service.refrescar(new RefreshRequestDto("bueno"))).isInstanceOf(SesionInvalidaException.class);
        assertThat(activa.getMotivoCierre()).isEqualTo(AuthServiceImpl.CIERRE_CLIENTE_INACTIVO);

        SesionCliente otra = sesion("s3", null);
        when(sesionRepository.findByRefreshHash(AuthServiceImpl.sha256("otro"))).thenReturn(Optional.of(otra));
        cliente.setActivo(true);
        var tokens = service.refrescar(new RefreshRequestDto("otro"));
        assertThat(otra.getRefreshHash()).isEqualTo(AuthServiceImpl.sha256(tokens.getRefreshToken()));
        assertThat(tokens.getSesionId()).isEqualTo("s3");
    }

    @Test
    @DisplayName("Logout y cerrar sesión: solo sesiones propias; sin sesión en el token no hace nada")
    void logoutYCerrar() {
        service.logout(new ClienteAutenticado(7L, Set.of()));
        verify(sesionRepository, never()).findById(any());

        SesionCliente propia = sesion("s1", null);
        when(sesionRepository.findById("s1")).thenReturn(Optional.of(propia));
        service.logout(new ClienteAutenticado(7L, Set.of(), "s1"));
        assertThat(propia.getMotivoCierre()).isEqualTo(AuthServiceImpl.CIERRE_LOGOUT);

        SesionCliente ajena = sesion("s2", null);
        ajena.setClienteId(99L);
        when(sesionRepository.findById("s2")).thenReturn(Optional.of(ajena));
        assertThatThrownBy(() -> service.cerrarSesion(new ClienteAutenticado(7L, Set.of(), "s1"), "S2"))
                .isInstanceOf(SesionNoEncontradaException.class);

        SesionCliente movil = sesion("s3", null);
        when(sesionRepository.findById("s3")).thenReturn(Optional.of(movil));
        service.cerrarSesion(new ClienteAutenticado(7L, Set.of(), "s1"), "s3");
        assertThat(movil.getMotivoCierre()).isEqualTo(AuthServiceImpl.CIERRE_USUARIO);
        // Cerrar una sesión ya cerrada no la vuelve a tocar
        service.cerrarSesion(new ClienteAutenticado(7L, Set.of(), "s1"), "s3");
        assertThat(movil.getMotivoCierre()).isEqualTo(AuthServiceImpl.CIERRE_USUARIO);
    }

    @Test
    @DisplayName("verificarActiva: 401 si la sesión no existe, es ajena o está cerrada; actualiza el último uso")
    void verificarActiva() {
        when(sesionRepository.findById("no")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.verificarActiva(new ClienteAutenticado(7L, Set.of(), "no")))
                .isInstanceOf(AutenticacionClienteException.class);

        SesionCliente cerrada = sesion("c", AHORA);
        when(sesionRepository.findById("c")).thenReturn(Optional.of(cerrada));
        assertThatThrownBy(() -> service.verificarActiva(new ClienteAutenticado(7L, Set.of(), "c")))
                .isInstanceOf(AutenticacionClienteException.class);

        SesionCliente activa = sesion("a", null);
        when(sesionRepository.findById("a")).thenReturn(Optional.of(activa));
        service.verificarActiva(new ClienteAutenticado(7L, Set.of(), "a"));
        assertThat(activa.getUltimoUso()).isEqualTo(AHORA);
        service.verificarActiva(new ClienteAutenticado(7L, Set.of(), "a"));
        verify(sesionRepository).save(activa);
    }

    @Test
    @DisplayName("Alertas y perfil")
    void alertasYPerfil() {
        assertThat(service.alertasPendientes(new ClienteAutenticado(7L, Set.of()))).isEmpty();
        assertThat(service.marcarAlertasLeidas(new ClienteAutenticado(7L, Set.of()))).isZero();

        AlertaSesion alerta = new AlertaSesion();
        alerta.setId(1L);
        alerta.setSesionOrigenId("nueva");
        alerta.setTipoDispositivo(TipoDispositivo.MOVIL);
        alerta.setNombreDispositivo("Pixel");
        alerta.setFecha(AHORA);
        when(alertaRepository.findBySesionDestinoIdAndLeidaFalseOrderByFechaDesc("s1")).thenReturn(List.of(alerta));
        assertThat(service.alertasPendientes(new ClienteAutenticado(7L, Set.of(), "s1")))
                .singleElement().satisfies(a -> assertThat(a.getMensaje()).contains("la app móvil (Pixel)"));
        when(alertaRepository.marcarLeidas("s1")).thenReturn(1);
        assertThat(service.marcarAlertasLeidas(new ClienteAutenticado(7L, Set.of(), "s1"))).isEqualTo(1);

        assertThat(AuthServiceImpl.mensajeAlerta(TipoDispositivo.WEB, null)).startsWith("Se inició sesión en tu cuenta desde la web.");

        when(clienteRepository.findById(7L)).thenReturn(Optional.of(cliente));
        assertThat(service.perfil(new ClienteAutenticado(7L, Set.of())).getNumeroCuenta()).isNull();
        when(clienteRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.perfil(new ClienteAutenticado(8L, Set.of()))).isInstanceOf(AutenticacionClienteException.class);

        when(sesionRepository.findActivas(eq(7L), any())).thenReturn(List.of(sesion("s1", null), sesion("s2", null)));
        assertThat(service.sesiones(new ClienteAutenticado(7L, Set.of(), "s1")))
                .extracting(s -> s.isActual()).containsExactly(true, false);
    }
}
