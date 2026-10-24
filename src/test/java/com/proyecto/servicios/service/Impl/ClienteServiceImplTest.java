package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CorreoYaRegistradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteActualizarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteResponseDto;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de ClienteServiceImpl con repositorios simulados (sin base de datos real).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceImpl - Pruebas Unitarias")
class ClienteServiceImplTest {

    private static final String CURP = "GALJ900515HJCRPN09";
    private static final String RFC = "GALJ900515AB1";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Spy
    private ClienteMapper clienteMapper = new ClienteMapper();

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
        dto.setCurp(CURP);
        dto.setRfc(RFC);
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

    private Cliente buildClienteGuardado() {
        Cliente c = new ClienteMapper().toEntity(buildDtoValido());
        c.setId(1L);
        c.setActivo(true);
        c.setCreatedAt(LocalDateTime.now());
        c.getDomicilio().setId(1L);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(1L);
        cuenta.setNumeroCuenta("0000000001");
        cuenta.setSaldo(BigDecimal.valueOf(1000));
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(c);
        c.setCuenta(cuenta);
        return c;
    }

    private void sinDuplicados() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
    }

    private ClienteActualizarRequestDto buildActualizacion(String nombre) {
        ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
        dto.setIdentificadorFiscal(CURP);
        dto.setNombre(nombre);
        return dto;
    }

    // ─────────────────────────────────────────────
    //  1. registrar
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("1. registrar")
    class RegistrarTests {

        @Test
        @DisplayName("Datos válidos: guarda y devuelve un DTO de respuesta")
        void registrar_datosValidos_devuelveDto() {
            sinDuplicados();
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            ClienteResponseDto resultado = clienteService.registrar(buildDtoValido());

            assertThat(resultado.getCurp()).isEqualTo(CURP);
            assertThat(resultado.getActivo()).isTrue();
            assertThat(resultado.getCuenta().getEstatus()).isEqualTo("ACTIVA");
            assertThat(resultado.getCuenta().getSaldo()).isEqualByComparingTo("1000.00");
            assertThat(resultado.getDomicilio().getCodigoPostal()).isEqualTo("44100");
        }

        @Test
        @DisplayName("El servidor asigna activo, cuenta y relación con domicilio")
        void registrar_camposControladosPorServidor() {
            sinDuplicados();
            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            when(clienteRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrar(buildDtoValido());

            Cliente guardado = captor.getValue();
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.getActivo()).isTrue();
            assertThat(guardado.getCuenta().getNumeroCuenta()).matches("\\d{10}");
            assertThat(guardado.getCuenta().getCliente()).isSameAs(guardado);
            assertThat(guardado.getDomicilio().getCliente()).isSameAs(guardado);
        }

        @Test
        @DisplayName("Menor de edad lanza ReglaNegocioException y no guarda")
        void registrar_menorDeEdad_lanzaExcepcion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().minusYears(18).plusDays(1));

            assertThatThrownBy(() -> clienteService.registrar(dto))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("mayor de edad");
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Exactamente 18 años se registra sin error")
        void registrar_exactamente18_seRegistra() {
            sinDuplicados();
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().minusYears(18));

            assertThatCode(() -> clienteService.registrar(dto)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CURP duplicada lanza ClienteYaRegistradoException")
        void registrar_curpDuplicada() {
            when(clienteRepository.existsByCurp(CURP)).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrar(buildDtoValido()))
                    .isInstanceOf(ClienteYaRegistradoException.class)
                    .hasMessageContaining("CURP");
        }

        @Test
        @DisplayName("RFC duplicado lanza ClienteYaRegistradoException")
        void registrar_rfcDuplicado() {
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(RFC)).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrar(buildDtoValido()))
                    .isInstanceOf(ClienteYaRegistradoException.class)
                    .hasMessageContaining("RFC");
        }

        @Test
        @DisplayName("Correo duplicado lanza CorreoYaRegistradoException")
        void registrar_correoDuplicado() {
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo("juan.garcia@email.com")).thenReturn(true);

            assertThatThrownBy(() -> clienteService.registrar(buildDtoValido()))
                    .isInstanceOf(CorreoYaRegistradoException.class)
                    .hasMessageContaining("juan.garcia@email.com");
        }

        @Test
        @DisplayName("Número de cuenta repetido se regenera hasta ser único")
        void registrar_numeroCuentaRepetido_seRegenera() {
            when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
            when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
            when(clienteRepository.existsByCorreo(anyString())).thenReturn(false);
            when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(true, true, false);
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            clienteService.registrar(buildDtoValido());

            verify(cuentaRepository, times(3)).existsByNumeroCuenta(anyString());
        }
    }

    // ─────────────────────────────────────────────
    //  2. buscar
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("2. buscar")
    class BuscarTests {

        @Test
        @DisplayName("Convierte las entidades encontradas a DTO de respuesta")
        @SuppressWarnings("unchecked")
        void buscar_devuelveDtos() {
            when(clienteRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(buildClienteGuardado()));

            ClienteConsultaRequestDto filtros = new ClienteConsultaRequestDto();
            filtros.setNombre("jua");
            List<ClienteResponseDto> resultado = clienteService.buscar(filtros);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getNombre()).isEqualTo("Juan");
        }

        @Test
        @DisplayName("Sin coincidencias devuelve lista vacía")
        @SuppressWarnings("unchecked")
        void buscar_sinResultados_listaVacia() {
            when(clienteRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

            assertThat(clienteService.buscar(new ClienteConsultaRequestDto())).isEmpty();
        }
    }

    // ─────────────────────────────────────────────
    //  3. actualizar
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("3. actualizar")
    class ActualizarTests {

        @Test
        @DisplayName("Actualiza sólo los campos enviados")
        void actualizar_soloCamposEnviados() {
            Cliente existente = buildClienteGuardado();
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClienteResponseDto resultado = clienteService.actualizar(buildActualizacion("Carlos"));

            assertThat(resultado.getNombre()).isEqualTo("Carlos");
            assertThat(resultado.getApellidoPaterno()).isEqualTo("García");
            assertThat(resultado.getCorreo()).isEqualTo("juan.garcia@email.com");
        }

        @Test
        @DisplayName("No modifica CURP, RFC, fecha de nacimiento ni estatus")
        void actualizar_noModificaCamposDelServidor() {
            Cliente existente = buildClienteGuardado();
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClienteResponseDto resultado = clienteService.actualizar(buildActualizacion("Carlos"));

            assertThat(resultado.getCurp()).isEqualTo(CURP);
            assertThat(resultado.getRfc()).isEqualTo(RFC);
            assertThat(resultado.getFechaNacimiento()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(resultado.getActivo()).isTrue();
        }

        @Test
        @DisplayName("Actualiza el domicilio cuando se envía")
        void actualizar_domicilio() {
            Cliente existente = buildClienteGuardado();
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClienteActualizarRequestDto dto = new ClienteActualizarRequestDto();
            dto.setIdentificadorFiscal(CURP);
            DomicilioDto dom = buildDtoValido().getDomicilio();
            dom.setCalle("Calle Nueva");
            dom.setCodigoPostal("12345");
            dto.setDomicilio(dom);

            ClienteResponseDto resultado = clienteService.actualizar(dto);

            assertThat(resultado.getDomicilio().getCalle()).isEqualTo("Calle Nueva");
            assertThat(resultado.getDomicilio().getCodigoPostal()).isEqualTo("12345");
        }

        @Test
        @DisplayName("Correo de otro cliente lanza CorreoYaRegistradoException")
        void actualizar_correoDeOtroCliente() {
            Cliente existente = buildClienteGuardado();
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(existente));
            when(clienteRepository.existsByCorreoAndIdNot("otro@email.com", 1L)).thenReturn(true);

            ClienteActualizarRequestDto dto = buildActualizacion(null);
            dto.setCorreo("otro@email.com");

            assertThatThrownBy(() -> clienteService.actualizar(dto))
                    .isInstanceOf(CorreoYaRegistradoException.class);
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cliente dado de baja no se puede actualizar")
        void actualizar_clienteInactivo() {
            Cliente existente = buildClienteGuardado();
            existente.setActivo(false);
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> clienteService.actualizar(buildActualizacion("Carlos")))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("Cliente inexistente lanza ClienteNoEncontradoException")
        void actualizar_inexistente() {
            when(clienteRepository.findFirstByCurpOrRfc(anyString(), anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.actualizar(buildActualizacion("Carlos")))
                    .isInstanceOf(ClienteNoEncontradoException.class)
                    .hasMessageContaining(CURP);
        }
    }

    // ─────────────────────────────────────────────
    //  4. darDeBaja
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("4. darDeBaja")
    class BajaTests {

        @Test
        @DisplayName("Desactiva al cliente y su cuenta sin borrarlos")
        void darDeBaja_desactivaClienteYCuenta() {
            Cliente c = buildClienteGuardado();
            when(clienteRepository.findFirstByCurpOrRfc(RFC, RFC)).thenReturn(Optional.of(c));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClienteResponseDto resultado = clienteService.darDeBaja(RFC);

            assertThat(resultado.getActivo()).isFalse();
            assertThat(resultado.getCuenta().getEstatus()).isEqualTo("INACTIVA");
            verify(clienteRepository, never()).delete(any(Cliente.class));
        }

        @Test
        @DisplayName("Cliente sin cuenta sólo se desactiva")
        void darDeBaja_sinCuenta() {
            Cliente c = buildClienteGuardado();
            c.setCuenta(null);
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(c));
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(clienteService.darDeBaja(CURP).getActivo()).isFalse();
        }

        @Test
        @DisplayName("Cliente ya dado de baja lanza ReglaNegocioException")
        void darDeBaja_yaInactivo() {
            Cliente c = buildClienteGuardado();
            c.setActivo(false);
            when(clienteRepository.findFirstByCurpOrRfc(CURP, CURP)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> clienteService.darDeBaja(CURP))
                    .isInstanceOf(ReglaNegocioException.class);
        }

        @Test
        @DisplayName("Cliente inexistente lanza ClienteNoEncontradoException")
        void darDeBaja_inexistente() {
            when(clienteRepository.findFirstByCurpOrRfc(anyString(), anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clienteService.darDeBaja(CURP))
                    .isInstanceOf(ClienteNoEncontradoException.class);
        }
    }
}
