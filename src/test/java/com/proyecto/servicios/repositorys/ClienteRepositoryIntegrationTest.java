package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para ClienteRepository y CuentaRepository.
 * Usa H2 en memoria con el perfil "test".
 * Spring levanta solo el slice JPA (@DataJpaTest).
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ClienteRepository / CuentaRepository - Pruebas de Repositorio")
class ClienteRepositoryIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    private Cliente clienteGuardado;

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    private Cliente crearCliente(String curp, String rfc, String correo, String numeroCuenta) {
        Cliente c = new Cliente();
        c.setNombre("Test");
        c.setApellidoPaterno("Apellido");
        c.setApellidoMaterno("Materno");
        c.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        c.setCurp(curp);
        c.setRfc(rfc);
        c.setSexo("M");
        c.setNacionalidad("Mexicana");
        c.setEstadoCivil("Soltero");
        c.setCorreo(correo);
        c.setTelefonoMovil("3300000000");
        c.setOcupacion("QA");
        c.setEmpresa("TestCo");
        c.setIngresoMensual(BigDecimal.valueOf(10000));
        c.setActivo(true);
        c.setCreatedAt(LocalDateTime.now());

        Domicilio dom = new Domicilio();
        dom.setCalle("Calle Test");
        dom.setNumeroExterior("1");
        dom.setColonia("Colonia");
        dom.setMunicipio("Municipio");
        dom.setEstado("Estado");
        dom.setCodigoPostal("00000");
        dom.setPais("México");
        dom.setCliente(c);
        c.setDomicilio(dom);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(numeroCuenta);
        cuenta.setSaldo(BigDecimal.valueOf(1000));
        cuenta.setEstatus("ACTIVA");
        cuenta.setCreatedAt(LocalDateTime.now());
        cuenta.setCliente(c);
        c.setCuenta(cuenta);

        return clienteRepository.save(c);
    }

    @BeforeEach
    void setUp() {
        clienteGuardado = crearCliente(
                "AAAA900101HDFBBB01",
                "AAAA900101AB1",
                "test@test.com",
                "1000000001"
        );
    }

    // ─────────────────────────────────────────────
    //  1. Búsqueda por CURP
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Búsqueda por CURP")
    class BusquedaCurpTests {

        @Test
        @DisplayName("findByCurp con CURP existente retorna el cliente")
        void findByCurp_existente_retornaCliente() {
            Optional<Cliente> resultado = clienteRepository.findByCurp("AAAA900101HDFBBB01");
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCurp()).isEqualTo("AAAA900101HDFBBB01");
        }

        @Test
        @DisplayName("findByCurp con CURP inexistente retorna Optional vacío")
        void findByCurp_inexistente_retornaVacio() {
            assertThat(clienteRepository.findByCurp("AAAA000000HDFBBB00")).isEmpty();
        }

        @Test
        @DisplayName("existsByCurp con CURP registrada retorna true")
        void existsByCurp_registrada_retornaTrue() {
            assertThat(clienteRepository.existsByCurp("AAAA900101HDFBBB01")).isTrue();
        }

        @Test
        @DisplayName("existsByCurp con CURP no registrada retorna false")
        void existsByCurp_noRegistrada_retornaFalse() {
            assertThat(clienteRepository.existsByCurp("AAAA000000HDFBBB00")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  2. Búsqueda por RFC
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Búsqueda por RFC")
    class BusquedaRfcTests {

        @Test
        @DisplayName("findByRfc con RFC existente retorna el cliente")
        void findByRfc_existente_retornaCliente() {
            Optional<Cliente> resultado = clienteRepository.findByRfc("AAAA900101AB1");
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getRfc()).isEqualTo("AAAA900101AB1");
        }

        @Test
        @DisplayName("findByRfc con RFC inexistente retorna Optional vacío")
        void findByRfc_inexistente_retornaVacio() {
            assertThat(clienteRepository.findByRfc("XXXXXXXXXXX")).isEmpty();
        }

        @Test
        @DisplayName("existsByRfc con RFC registrado retorna true")
        void existsByRfc_registrado_retornaTrue() {
            assertThat(clienteRepository.existsByRfc("AAAA900101AB1")).isTrue();
        }

        @Test
        @DisplayName("existsByRfc con RFC no registrado retorna false")
        void existsByRfc_noRegistrado_retornaFalse() {
            assertThat(clienteRepository.existsByRfc("NOTEXISTS0001")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  3. Búsqueda por Correo
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Búsqueda por Correo")
    class BusquedaCorreoTests {

        @Test
        @DisplayName("findByCorreo con correo existente retorna el cliente")
        void findByCorreo_existente_retornaCliente() {
            Optional<Cliente> resultado = clienteRepository.findByCorreo("test@test.com");
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getCorreo()).isEqualTo("test@test.com");
        }

        @Test
        @DisplayName("findByCorreo con correo inexistente retorna Optional vacío")
        void findByCorreo_inexistente_retornaVacio() {
            assertThat(clienteRepository.findByCorreo("noexiste@test.com")).isEmpty();
        }

        @Test
        @DisplayName("existsByCorreo con correo registrado retorna true")
        void existsByCorreo_registrado_retornaTrue() {
            assertThat(clienteRepository.existsByCorreo("test@test.com")).isTrue();
        }

        @Test
        @DisplayName("existsByCorreo con correo no registrado retorna false")
        void existsByCorreo_noRegistrado_retornaFalse() {
            assertThat(clienteRepository.existsByCorreo("noregistrado@x.com")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  4. Clientes activos
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Clientes activos")
    class ClientesActivosTests {

        @Test
        @DisplayName("findByActivoTrue retorna solo los clientes activos")
        void findByActivoTrue_retornaSoloActivos() {
            // Crear uno inactivo
            Cliente inactivo = crearCliente(
                    "BBBB900101HDFCCC01",
                    "BBBB900101XY1",
                    "inactivo@test.com",
                    "2000000002"
            );
            inactivo.setActivo(false);
            clienteRepository.save(inactivo);

            List<Cliente> activos = clienteRepository.findByActivoTrue();
            assertThat(activos).isNotEmpty();
            assertThat(activos).allMatch(Cliente::getActivo);
        }

        @Test
        @DisplayName("Todos activos — findByActivoTrue retorna todos")
        void findByActivoTrue_todosActivos_retornaTodos() {
            List<Cliente> activos = clienteRepository.findByActivoTrue();
            assertThat(activos).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    // ─────────────────────────────────────────────
    //  5. Consulta por rango de fechas
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Consulta por rango de fechas")
    class RangoFechasTests {

        @Test
        @DisplayName("findClientesByRangoFechas incluye clientes en el rango")
        void findClientesByRangoFechas_retornaClientesDentroDelRango() {
            LocalDateTime inicio = LocalDateTime.now().minusDays(1);
            LocalDateTime fin = LocalDateTime.now().plusDays(1);
            List<Cliente> resultado = clienteRepository.findClientesByRangoFechas(inicio, fin);
            assertThat(resultado).isNotEmpty();
        }

        @Test
        @DisplayName("findClientesByRangoFechas excluye clientes fuera del rango")
        void findClientesByRangoFechas_excluye_clientesFueraRango() {
            LocalDateTime inicio = LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime fin = LocalDateTime.of(2000, 12, 31, 23, 59);
            List<Cliente> resultado = clienteRepository.findClientesByRangoFechas(inicio, fin);
            assertThat(resultado).isEmpty();
        }
    }

    // ─────────────────────────────────────────────
    //  6. Cuenta
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("CuentaRepository")
    class CuentaRepositoryTests {

        @Test
        @DisplayName("findByNumeroCuenta con número existente retorna cuenta")
        void findByNumeroCuenta_existente_retornaCuenta() {
            Optional<Cuenta> resultado = cuentaRepository.findByNumeroCuenta("1000000001");
            assertThat(resultado).isPresent();
            assertThat(resultado.get().getNumeroCuenta()).isEqualTo("1000000001");
        }

        @Test
        @DisplayName("findByNumeroCuenta con número inexistente retorna Optional vacío")
        void findByNumeroCuenta_inexistente_retornaVacio() {
            assertThat(cuentaRepository.findByNumeroCuenta("9999999999")).isEmpty();
        }

        @Test
        @DisplayName("existsByNumeroCuenta con número registrado retorna true")
        void existsByNumeroCuenta_registrado_retornaTrue() {
            assertThat(cuentaRepository.existsByNumeroCuenta("1000000001")).isTrue();
        }

        @Test
        @DisplayName("existsByNumeroCuenta con número no registrado retorna false")
        void existsByNumeroCuenta_noRegistrado_retornaFalse() {
            assertThat(cuentaRepository.existsByNumeroCuenta("0000000000")).isFalse();
        }

        @Test
        @DisplayName("findByEstatus 'ACTIVA' retorna cuentas activas")
        void findByEstatus_activa_retornaCuentasActivas() {
            List<Cuenta> activas = cuentaRepository.findByEstatus("ACTIVA");
            assertThat(activas).isNotEmpty();
            assertThat(activas).allMatch(c -> "ACTIVA".equals(c.getEstatus()));
        }

        @Test
        @DisplayName("findByEstatus 'INACTIVA' sin inactivas retorna lista vacía")
        void findByEstatus_inactiva_sinInactivas_listaVacia() {
            List<Cuenta> inactivas = cuentaRepository.findByEstatus("INACTIVA");
            assertThat(inactivas).isEmpty();
        }

        @Test
        @DisplayName("Cuenta queda asociada al cliente correcto")
        void cuenta_asociadaAlClienteCorrecto() {
            Optional<Cuenta> cuenta = cuentaRepository.findByNumeroCuenta("1000000001");
            assertThat(cuenta).isPresent();
            assertThat(cuenta.get().getCliente().getId()).isEqualTo(clienteGuardado.getId());
        }

        @Test
        @DisplayName("Saldo de la cuenta no es negativo")
        void cuenta_saldo_noEsNegativo() {
            List<Cuenta> cuentas = cuentaRepository.findAll();
            assertThat(cuentas).allMatch(c -> c.getSaldo().compareTo(BigDecimal.ZERO) >= 0);
        }
    }

    // ─────────────────────────────────────────────
    //  7. Unicidad de campos (restricción BD)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Restricciones de unicidad")
    class UnicidadTests {

        @Test
        @DisplayName("No puede existir dos clientes con la misma CURP")
        void curp_duplicada_lanzaExcepcion() {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                crearCliente(
                        "AAAA900101HDFBBB01", // CURP duplicada
                        "CCCC900101XZ1",
                        "nuevo@test.com",
                        "3000000003"
                );
            });
        }

        @Test
        @DisplayName("No puede existir dos clientes con el mismo RFC")
        void rfc_duplicado_lanzaExcepcion() {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                crearCliente(
                        "CCCC900101HDFCCC01",
                        "AAAA900101AB1", // RFC duplicado
                        "nuevo2@test.com",
                        "4000000004"
                );
            });
        }

        @Test
        @DisplayName("No puede existir dos clientes con el mismo correo")
        void correo_duplicado_lanzaExcepcion() {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                crearCliente(
                        "DDDD900101HDFCCC01",
                        "DDDD900101XZ1",
                        "test@test.com", // correo duplicado
                        "5000000005"
                );
            });
        }

        @Test
        @DisplayName("No puede existir dos cuentas con el mismo número")
        void numeroCuenta_duplicado_lanzaExcepcion() {
            org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
                crearCliente(
                        "EEEE900101HDFCCC01",
                        "EEEE900101XZ1",
                        "unico@test.com",
                        "1000000001" // número de cuenta duplicado
                );
            });
        }
    }
}
