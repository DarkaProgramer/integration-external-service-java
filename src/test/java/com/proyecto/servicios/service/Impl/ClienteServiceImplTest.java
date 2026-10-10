package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias exhaustivas para ClienteServiceImpl.
 * Se usan mocks de los repositorios (sin base de datos real).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceImpl - Pruebas Unitarias")
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private ClienteRegistroRequestDto buildDtoValido() {
        DomicilioDto dom = new DomicilioDto();
        dom.setCalle("Av. Revolución");
        dom.setNumeroExterior("123");
        dom.setColonia("Centro");
        dom.setMunicipio("Guadalajara");
        dom.setEstado("Jalisco");
        dom.setCodigoPostal("44100");
        dom.setPais("México");

        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setNombre("Juan");
        dto.setApellidoPaterno("García");
        dto.setApellidoMaterno("López");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        dto.setCurp("GALJ900515HJCRPN09");
        dto.setRfc("GALJ900515AB1");
        dto.setSexo("M");
        dto.setNacionalidad("Mexicana");
        dto.setEstadoCivil("Soltero");
        dto.setCorreo("juan.garcia@email.com");
        dto.setTelefonoMovil("3312345678");
        dto.setOcupacion("Ingeniero");
        dto.setEmpresa("TechCorp");
        dto.setIngresoMensual(BigDecimal.valueOf(20000));
        dto.setDomicilio(dom);
        return dto;
    }

    private Cliente buildClienteGuardado(ClienteRegistroRequestDto dto) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNombre(dto.getNombre());
        c.setApellidoPaterno(dto.getApellidoPaterno());
        c.setApellidoMaterno(dto.getApellidoMaterno());
        c.setFechaNacimiento(dto.getFechaNacimiento());
        c.setCurp(dto.getCurp());
        c.setRfc(dto.getRfc());
        c.setSexo(dto.getSexo());
        c.setNacionalidad(dto.getNacionalidad());
        c.setEstadoCivil(dto.getEstadoCivil());
        c.setCorreo(dto.getCorreo());
        c.setTelefonoMovil(dto.getTelefonoMovil());
        c.setOcupacion(dto.getOcupacion());
        c.setEmpresa(dto.getEmpresa());
        c.setIngresoMensual(dto.getIngresoMensual());
        c.setActivo(true);
        c.setCreatedAt(LocalDateTime.now());

        Domicilio dom = new Domicilio();
        dom.setId(1L);
        dom.setCalle("Av. Revolución");
        dom.setNumeroExterior("123");
        dom.setColonia("Centro");
        dom.setMunicipio("Guadalajara");
        dom.setEstado("Jalisco");
        dom.setCodigoPostal("44100");
        dom.setPais("México");
        dom.setCliente(c);
        c.setDomicilio(dom);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("0000000001");
        cuenta.setSaldo(BigDecimal.valueOf(1000));
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(c);
        c.setCuenta(cuenta);

        return c;
    }

    // ─────────────────────────────────────────────
    //  1. registrarCliente
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("1. registrarCliente")
    class RegistrarClienteTests {

        @Test
        @DisplayName("Registro exitoso - cliente válido mayor de edad")
        void registrarCliente_datosValidos_guardaYRetornaCliente() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            Cliente esperado = buildClienteGuardado(dto);

            when(clienteRepository.existsByCurp(dto.getCurp())).thenReturn(false);
            when(clienteRepository.existsByRfc(dto.getRfc())).thenReturn(false);
            when(clienteRepository.existsByCorreo(dto.getCorreo())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenReturn(esperado);

            Cliente resultado = clienteService.registrarCliente(dto);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getNombre()).isEqualTo("Juan");
            assertThat(resultado.getCurp()).isEqualTo(dto.getCurp());
            assertThat(resultado.getCuenta()).isNotNull();
            assertThat(resultado.getCuenta().getEstatus()).isEqualTo("ACTIVA");
            assertThat(resultado.getCuenta().getSaldo()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            verify(clienteRepository).save(any(Cliente.class));
        }

        @Test
        @DisplayName("La cuenta creada tiene estatus ACTIVA")
        void registrarCliente_cuentaCreada_tieneEstatusActiva() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrarCliente(dto);

            Cliente guardado = captor.getValue();
            assertThat(guardado.getCuenta()).isNotNull();
            assertThat(guardado.getCuenta().getEstatus()).isEqualTo("ACTIVA");
        }

        @Test
        @DisplayName("El saldo inicial de la cuenta no es negativo")
        void registrarCliente_saldoInicial_noEsNegativo() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrarCliente(dto);

            assertThat(captor.getValue().getCuenta().getSaldo())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("El domicilio queda asociado al cliente")
        void registrarCliente_domicilioAsociadoAlCliente() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrarCliente(dto);

            assertThat(captor.getValue().getDomicilio()).isNotNull();
            assertThat(captor.getValue().getDomicilio().getCalle()).isEqualTo("Av. Revolución");
        }

        @Test
        @DisplayName("El cliente queda activo al registrarse")
        void registrarCliente_clienteQueActivo() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrarCliente(dto);

            assertThat(captor.getValue().getActivo()).isTrue();
        }

        // ── Reglas de negocio: mayoría de edad ──

        @Test
        @DisplayName("Menor de edad lanza IllegalArgumentException")
        void registrarCliente_menorDeEdad_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().minusYears(17));

            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("mayor de edad");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Exactamente 18 años - debe registrarse sin error")
        void registrarCliente_exactamente18Anios_seRegistra() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().minusYears(18));

            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> clienteService.registrarCliente(dto)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("17 años y 364 días - debe lanzar excepción")
        void registrarCliente_casiBorde18Anios_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().minusYears(18).plusDays(1));

            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        // ── Reglas de negocio: duplicados ──

        @Test
        @DisplayName("CURP duplicada lanza ClienteYaRegistradoException")
        void registrarCliente_curpDuplicada_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(dto.getCurp())).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(ClienteYaRegistradoException.class)
                    .hasMessageContaining("CURP");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("RFC duplicado lanza ClienteYaRegistradoException")
        void registrarCliente_rfcDuplicado_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(dto.getRfc())).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(ClienteYaRegistradoException.class)
                    .hasMessageContaining("RFC");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Correo duplicado lanza ClienteYaRegistradoException")
        void registrarCliente_correoDuplicado_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(dto.getCorreo())).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrarCliente(dto))
                    .isInstanceOf(ClienteYaRegistradoException.class)
                    .hasMessageContaining("correo");

            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("El número de cuenta generado es único (intenta de nuevo si colisiona)")
        void registrarCliente_numeroCuenta_unicidad() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            // Primera llamada: colisión; segunda: libre
            when(cuentaRepository.existsByNumeroCuenta(anyString()))
                    .thenReturn(true).thenReturn(false);

            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrarCliente(dto);

            // Se llamó al menos 2 veces (buscó un número libre)
            verify(cuentaRepository, atLeast(2)).existsByNumeroCuenta(anyString());
            assertThat(captor.getValue().getCuenta().getNumeroCuenta()).isNotBlank();
        }
    }

    // ─────────────────────────────────────────────
    //  2. Consultas
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("2. Consultas")
    class ConsultasTests {

        @Test
        @DisplayName("consultarTodosClientes devuelve lista completa")
        void consultarTodosClientes_devuelveLista() {
            List<Cliente> lista = List.of(new Cliente(), new Cliente());
            when(clienteRepository.findAll()).thenReturn(lista);

            assertThat(clienteService.consultarTodosClientes()).hasSize(2);
        }

        @Test
        @DisplayName("consultarTodosClientes con BD vacía devuelve lista vacía")
        void consultarTodosClientes_sinRegistros_listaVacia() {
            when(clienteRepository.findAll()).thenReturn(List.of());

            assertThat(clienteService.consultarTodosClientes()).isEmpty();
        }

        @Test
        @DisplayName("consultarClientePorId existente devuelve cliente")
        void consultarClientePorId_existente_devuelveCliente() {
            Cliente c = new Cliente();
            c.setId(1L);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

            assertThat(clienteService.consultarClientePorId(1L).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("consultarClientePorId inexistente lanza ClienteNoEncontradoException")
        void consultarClientePorId_inexistente_lanzaExcepcion() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.consultarClientePorId(99L))
                    .isInstanceOf(ClienteNoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("consultarClientePorCurp existente devuelve cliente")
        void consultarClientePorCurp_existente_devuelveCliente() {
            Cliente c = new Cliente();
            c.setCurp("GALJ900515HJCRPN09");
            when(clienteRepository.findByCurp("GALJ900515HJCRPN09")).thenReturn(Optional.of(c));

            assertThat(clienteService.consultarClientePorCurp("GALJ900515HJCRPN09").getCurp())
                    .isEqualTo("GALJ900515HJCRPN09");
        }

        @Test
        @DisplayName("consultarClientePorCurp inexistente lanza excepción")
        void consultarClientePorCurp_inexistente_lanzaExcepcion() {
            when(clienteRepository.findByCurp("INVALIDA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.consultarClientePorCurp("INVALIDA"))
                    .isInstanceOf(ClienteNoEncontradoException.class)
                    .hasMessageContaining("CURP");
        }

        @Test
        @DisplayName("consultarClientePorRfc existente devuelve cliente")
        void consultarClientePorRfc_existente_devuelveCliente() {
            Cliente c = new Cliente();
            c.setRfc("GALJ900515AB1");
            when(clienteRepository.findByRfc("GALJ900515AB1")).thenReturn(Optional.of(c));

            assertThat(clienteService.consultarClientePorRfc("GALJ900515AB1").getRfc())
                    .isEqualTo("GALJ900515AB1");
        }

        @Test
        @DisplayName("consultarClientePorRfc inexistente lanza excepción")
        void consultarClientePorRfc_inexistente_lanzaExcepcion() {
            when(clienteRepository.findByRfc("RFCINVALIDO")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.consultarClientePorRfc("RFCINVALIDO"))
                    .isInstanceOf(ClienteNoEncontradoException.class)
                    .hasMessageContaining("RFC");
        }

        @Test
        @DisplayName("consultarClientePorCorreo existente devuelve cliente")
        void consultarClientePorCorreo_existente_devuelveCliente() {
            Cliente c = new Cliente();
            c.setCorreo("test@test.com");
            when(clienteRepository.findByCorreo("test@test.com")).thenReturn(Optional.of(c));

            assertThat(clienteService.consultarClientePorCorreo("test@test.com").getCorreo())
                    .isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("consultarClientePorCorreo inexistente lanza excepción")
        void consultarClientePorCorreo_inexistente_lanzaExcepcion() {
            when(clienteRepository.findByCorreo("noexiste@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.consultarClientePorCorreo("noexiste@x.com"))
                    .isInstanceOf(ClienteNoEncontradoException.class)
                    .hasMessageContaining("correo");
        }

        @Test
        @DisplayName("consultarCuentaPorNumero existente devuelve cuenta")
        void consultarCuentaPorNumero_existente_devuelveCuenta() {
            Cuenta cuenta = new Cuenta();
            cuenta.setNumeroCuenta("1234567890");
            when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta));

            assertThat(clienteService.consultarCuentaPorNumero("1234567890").getNumeroCuenta())
                    .isEqualTo("1234567890");
        }

        @Test
        @DisplayName("consultarCuentaPorNumero inexistente lanza CuentaNoEncontradaException")
        void consultarCuentaPorNumero_inexistente_lanzaExcepcion() {
            when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.consultarCuentaPorNumero("0000000000"))
                    .isInstanceOf(CuentaNoEncontradaException.class);
        }

        @Test
        @DisplayName("consultarClientesActivos filtra solo activos")
        void consultarClientesActivos_devuelveSoloActivos() {
            Cliente activo = new Cliente();
            activo.setActivo(true);
            when(clienteRepository.findByActivoTrue()).thenReturn(List.of(activo));

            List<Cliente> resultado = clienteService.consultarClientesActivos();
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getActivo()).isTrue();
        }

        @Test
        @DisplayName("consultarCuentasActivas filtra cuentas ACTIVA")
        void consultarCuentasActivas_devuelveSoloActivas() {
            Cuenta activa = new Cuenta();
            activa.setEstatus("ACTIVA");
            when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(activa));

            List<Cuenta> resultado = clienteService.consultarCuentasActivas();
            assertThat(resultado).allMatch(cu -> "ACTIVA".equals(cu.getEstatus()));
        }

        @Test
        @DisplayName("consultarClientesPorRangoFechas retorna clientes en el rango")
        void consultarClientesPorRangoFechas_retornaClientes() {
            LocalDateTime inicio = LocalDateTime.of(2025, 1, 1, 0, 0);
            LocalDateTime fin = LocalDateTime.of(2025, 12, 31, 23, 59);
            Cliente c = new Cliente();
            when(clienteRepository.findClientesByRangoFechas(inicio, fin)).thenReturn(List.of(c));

            assertThat(clienteService.consultarClientesPorRangoFechas(inicio, fin)).hasSize(1);
        }

        @Test
        @DisplayName("consultarClientesPorRangoFechas sin resultados retorna lista vacía")
        void consultarClientesPorRangoFechas_sinResultados_listaVacia() {
            LocalDateTime inicio = LocalDateTime.of(2010, 1, 1, 0, 0);
            LocalDateTime fin = LocalDateTime.of(2010, 12, 31, 23, 59);
            when(clienteRepository.findClientesByRangoFechas(inicio, fin)).thenReturn(List.of());

            assertThat(clienteService.consultarClientesPorRangoFechas(inicio, fin)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────
    //  3. actualizarCliente
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("3. actualizarCliente")
    class ActualizarClienteTests {

        @Test
        @DisplayName("Actualización exitosa modifica datos permitidos")
        void actualizarCliente_datosValidos_actualizaCliente() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("Carlos");
            dto.setCorreo("carlos@nuevo.com");

            Cliente existente = buildClienteGuardado(buildDtoValido());
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Cliente resultado = clienteService.actualizarCliente(1L, dto);

            assertThat(resultado.getNombre()).isEqualTo("Carlos");
            assertThat(resultado.getCorreo()).isEqualTo("carlos@nuevo.com");
        }

        @Test
        @DisplayName("Actualización no modifica CURP ni RFC")
        void actualizarCliente_noCambiaCurpNiRfc() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            String curpOriginal = "GALJ900515HJCRPN09";
            String rfcOriginal = "GALJ900515AB1";

            Cliente existente = buildClienteGuardado(buildDtoValido());
            existente.setCurp(curpOriginal);
            existente.setRfc(rfcOriginal);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // El servicio NO actualiza CURP ni RFC desde el DTO
            Cliente resultado = clienteService.actualizarCliente(1L, dto);

            assertThat(resultado.getCurp()).isEqualTo(curpOriginal);
            assertThat(resultado.getRfc()).isEqualTo(rfcOriginal);
        }

        @Test
        @DisplayName("Actualización de domicilio actualiza campos correctamente")
        void actualizarCliente_actualizaDomicilio() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCalle("Calle Nueva");
            dto.getDomicilio().setCodigoPostal("12345");

            Cliente existente = buildClienteGuardado(buildDtoValido());
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Cliente resultado = clienteService.actualizarCliente(1L, dto);

            assertThat(resultado.getDomicilio().getCalle()).isEqualTo("Calle Nueva");
            assertThat(resultado.getDomicilio().getCodigoPostal()).isEqualTo("12345");
        }

        @Test
        @DisplayName("Actualización de cliente inexistente lanza excepción")
        void actualizarCliente_inexistente_lanzaExcepcion() {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.actualizarCliente(999L, buildDtoValido()))
                    .isInstanceOf(ClienteNoEncontradoException.class);
        }
    }

    // ─────────────────────────────────────────────
    //  4. bajaLogicaCliente
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("4. bajaLogicaCliente")
    class BajaLogicaTests {

        @Test
        @DisplayName("Baja lógica desactiva cliente y su cuenta")
        void bajaLogicaCliente_clienteConCuenta_desactivaAmbos() {
            Cliente c = buildClienteGuardado(buildDtoValido());
            c.setActivo(true);
            c.getCuenta().setEstatus("ACTIVA");

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.bajaLogicaCliente(1L);

            assertThat(c.getActivo()).isFalse();
            assertThat(c.getCuenta().getEstatus()).isEqualTo("INACTIVA");
            verify(clienteRepository).save(c);
        }

        @Test
        @DisplayName("Baja lógica de cliente sin cuenta solo desactiva cliente")
        void bajaLogicaCliente_sinCuenta_soloDesactivaCliente() {
            Cliente c = buildClienteGuardado(buildDtoValido());
            c.setActivo(true);
            c.setCuenta(null);

            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> clienteService.bajaLogicaCliente(1L)).doesNotThrowAnyException();
            assertThat(c.getActivo()).isFalse();
        }

        @Test
        @DisplayName("Baja lógica de cliente inexistente lanza excepción")
        void bajaLogicaCliente_inexistente_lanzaExcepcion() {
            when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.bajaLogicaCliente(999L))
                    .isInstanceOf(ClienteNoEncontradoException.class);
        }

        @Test
        @DisplayName("La información del cliente NO se elimina físicamente (baja lógica)")
        void bajaLogicaCliente_noEliminaFisicamente() {
            Cliente c = buildClienteGuardado(buildDtoValido());
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.bajaLogicaCliente(1L);

            verify(clienteRepository, never()).delete(any());
            verify(clienteRepository, never()).deleteById(any());
        }
    }
}
