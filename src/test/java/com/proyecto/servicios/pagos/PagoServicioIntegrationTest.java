package com.proyecto.servicios.pagos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.entity.pagos.TipoMovimiento;
import com.proyecto.servicios.entity.pagos.ValidacionReferencia;
import com.proyecto.servicios.repositorys.pagos.ValidacionReferenciaRepository;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.repositorys.pagos.MovimientoCuentaRepository;
import com.proyecto.servicios.repositorys.pagos.PagoServicioRepository;
import com.proyecto.servicios.repositorys.pagos.TarjetaGuardadaRepository;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.seguridad.TokenClienteService;
import com.proyecto.servicios.service.pagos.ConciliacionPagosService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo: HTTP → controller → servicio → base de datos (H2) → Gestopago simulado con un servidor HTTP
 * local (incluye Feign real, timeouts, encabezado Bearer y formato de los parámetros).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Pago de servicios - flujo completo")
class PagoServicioIntegrationTest {

    private static final String URL = "/api/pagos/servicios";
    private static final String CATALOGO_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESPONSE>
              <MENSAJE><CODIGO>0</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
              <PRODUCTOS>
                <producto servicio="Telcel" producto="Telcel $100" idServicio="1" idProducto="100" precio="100.00" tipoReferencia="NUMERO"/>
                <producto servicio="CFE" producto="Pago CFE" idServicio="5" idProducto="500" precio="0" tipoReferencia="NUMERO"/>
              </PRODUCTOS>
            </RESPONSE>
            """;

    private static final HttpServer GESTOPAGO;
    private static final AtomicReference<RespuestaSimulada> RESPUESTA_SEND_TX = new AtomicReference<>();
    private static final List<Map<String, String>> PETICIONES_SEND_TX = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> AUTHORIZATION_RECIBIDOS = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger SECUENCIA = new AtomicInteger();
    private static final long BASE_CUENTAS = System.currentTimeMillis() % 1_000_000_000L * 10;

    static {
        try {
            GESTOPAGO = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            GESTOPAGO.setExecutor(Executors.newCachedThreadPool());
            GESTOPAGO.createContext("/sistema/service/getProductList.do", ex -> responder(ex, 200, CATALOGO_XML));
            GESTOPAGO.createContext("/sistema/service/sendTx.do", PagoServicioIntegrationTest::sendTx);
            GESTOPAGO.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + GESTOPAGO.getAddress().getPort();
        registry.add("gestopago.products.url", () -> url);
        registry.add("gestopago.tx.url", () -> url);
        registry.add("gestopago.tx.bearer-token", () -> "token-secreto-gestopago");
        registry.add("gestopago.tx.codigos-rechazo", () -> "05,14");
        registry.add("spring.cloud.openfeign.client.config.gestoPagoTransacciones.read-timeout", () -> "800");
        registry.add("pagos.conciliacion.max-intentos", () -> "2");
        registry.add("pagos.conciliacion.pendiente-minutos", () -> "0");
    }

    @AfterAll
    static void detener() {
        GESTOPAGO.stop(0);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TarjetaGuardadaRepository tarjetaGuardadaRepository;
    @Autowired private TokenClienteService tokenClienteService;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private CuentaRepository cuentaRepository;
    @Autowired private PagoServicioRepository pagoRepository;
    @Autowired private MovimientoCuentaRepository movimientoRepository;
    @Autowired private ConciliacionPagosService conciliacionPagosService;
    @Autowired private ValidacionReferenciaRepository validacionRepository;

    private Cliente cliente;
    private String numeroCuenta;
    private String token;

    @BeforeEach
    void setUp() {
        PETICIONES_SEND_TX.clear();
        AUTHORIZATION_RECIBIDOS.clear();
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(200, "<RESPONSE><CODIGO>0</CODIGO><TEXTO>Aprobada</TEXTO><TRANSACCION>GP-777</TRANSACCION></RESPONSE>", 0));
        cliente = crearCliente(new BigDecimal("1000.00"), true, "ACTIVA");
        numeroCuenta = cliente.getCuenta().getNumeroCuenta();
        token = "Bearer " + tokenClienteService.emitir(cliente.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Pago aprobado con tarjeta: no toca el saldo, no registra movimientos y envía a Gestopago el Bearer y los parámetros")
    void pagoAprobado() throws Exception {
        String folio = folio(pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "100.00"), 201)
                .andExpect(jsonPath("$.datos.estatus").value("APROBADA"))
                .andExpect(jsonPath("$.datos.folioProveedor").value("GP-777"))
                .andExpect(jsonPath("$.datos.referencia").value("******5678"))
                .andExpect(header().string("Idempotent-Replayed", "false"))
                .andReturn());

        assertThat(saldo()).isEqualByComparingTo("1000.00");
        assertThat(AUTHORIZATION_RECIBIDOS).containsExactly("Bearer token-secreto-gestopago");
        assertThat(PETICIONES_SEND_TX).singleElement().satisfies(p -> {
            assertThat(p).containsEntry("idServicio", "1").containsEntry("idProducto", "100")
                    .containsEntry("referencia", "5512345678").containsEntry("montoPago", "100.00")
                    .containsEntry("idTransaccionCliente", folio);
        });
        PagoServicio pago = pagoRepository.findByFolio(folio).orElseThrow();
        assertThat(movimientoRepository.findByPagoIdOrderByIdAsc(pago.getId()))
                .isEmpty();
        assertThat(pago.getMetodoPago()).isEqualTo("TARJETA_DEBITO");
    }

    @Test
    @DisplayName("Misma Idempotency-Key: devuelve el mismo resultado sin cobrar ni enviar otra vez")
    void idempotencia() throws Exception {
        String llave = UUID.randomUUID().toString();
        String body = solicitud("1", "100", "5512345678", "100.00");
        String folio1 = folio(pagar(llave, body, 201).andReturn());
        String folio2 = folio(pagar(llave, body, 200).andExpect(header().string("Idempotent-Replayed", "true")).andReturn());

        assertThat(folio2).isEqualTo(folio1);
        assertThat(PETICIONES_SEND_TX).hasSize(1);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Misma Idempotency-Key con otros datos: 422 y sin cargo adicional")
    void idempotenciaConOtrosDatos() throws Exception {
        String llave = UUID.randomUUID().toString();
        pagar(llave, solicitud("1", "100", "5512345678", "100.00"), 201);
        pagar(llave, solicitud("1", "100", "5599999999", "100.00"), 422)
                .andExpect(jsonPath("$.error").value("Llave de idempotencia reutilizada"));
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Mismo pago con otra llave dentro de la ventana: 409 (doble clic)")
    void duplicadoConOtraLlave() throws Exception {
        String body = solicitud("1", "100", "5512345678", "100.00");
        pagar(UUID.randomUUID().toString(), body, 201);
        pagar(UUID.randomUUID().toString(), body, 409);
        assertThat(PETICIONES_SEND_TX).hasSize(1);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Timeout del proveedor: el monto queda retenido, 202 y EN_CONCILIACION (no se asume que falló)")
    void timeoutQuedaEnConciliacion() throws Exception {
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(200, "<RESPONSE><CODIGO>0</CODIGO></RESPONSE>", 1500));

        String folio = folio(pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "100.00"), 202)
                .andExpect(jsonPath("$.datos.estatus").value("EN_CONCILIACION"))
                .andReturn());

        assertThat(saldo()).isEqualByComparingTo("1000.00");
        assertThat(pagoRepository.findByFolio(folio).orElseThrow().getEstatus()).isEqualTo(EstatusPago.EN_CONCILIACION);
    }

    @Test
    @DisplayName("Conciliación sin consulta en el proveedor: no reenvía; tras los intentos pasa a revisión manual y el operador lo resuelve")
    void conciliacionYResolucionManual() throws Exception {
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(503, "Service Unavailable", 0));
        String folio = folio(pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "100.00"), 202).andReturn());

        conciliacionPagosService.conciliarPendientes();
        conciliacionPagosService.conciliarPendientes();

        assertThat(pagoRepository.findByFolio(folio).orElseThrow().getEstatus()).isEqualTo(EstatusPago.REVISION_MANUAL);
        assertThat(PETICIONES_SEND_TX).hasSize(1);
        assertThat(saldo()).isEqualByComparingTo("1000.00");

        // Un cliente normal no puede resolver
        mockMvc.perform(post(URL + "/conciliacion/resolver").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"" + folio + "\",\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isForbidden());

        String operador = "Bearer " + tokenClienteService.emitir(999L, List.of(ClienteAutenticado.PERMISO_CONCILIAR), Duration.ofMinutes(5));
        mockMvc.perform(post(URL + "/conciliacion/resolver").header("Authorization", operador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"" + folio + "\",\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estatus").value("RECHAZADA"));

        assertThat(saldo()).isEqualByComparingTo("1000.00");
        // Resolverlo otra vez no devuelve el dinero dos veces
        mockMvc.perform(post(URL + "/conciliacion/resolver").header("Authorization", operador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"" + folio + "\",\"resultado\":\"RECHAZADA\"}"))
                .andExpect(status().isConflict());
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Rechazo explícito del proveedor: 422, se reembolsa a la tarjeta y el saldo no cambia")
    void rechazoDevuelveFondos() throws Exception {
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(200, "{\"codigo\":\"05\",\"mensaje\":\"Referencia invalida\"}", 0));

        String folio = folio(pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "100.00"), 422)
                .andExpect(jsonPath("$.codigo").value(1))
                .andExpect(jsonPath("$.datos.estatus").value("RECHAZADA"))
                .andReturn());

        assertThat(saldo()).isEqualByComparingTo("1000.00");
        PagoServicio pago = pagoRepository.findByFolio(folio).orElseThrow();
        assertThat(movimientoRepository.findByPagoIdOrderByIdAsc(pago.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("Monto manipulado (no coincide con el precio del catálogo): 422 sin cargo ni llamada a sendTx")
    void montoManipulado() throws Exception {
        pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "1.00"), 422)
                .andExpect(jsonPath("$.error").value("Monto no permitido"));
        pagar(UUID.randomUUID().toString(), solicitud("9", "999", "5512345678", "100.00"), 422)
                .andExpect(jsonPath("$.error").value("Producto no válido"));
        pagar(UUID.randomUUID().toString(), solicitud("5", "500", "123456789012", "15000.00"), 422)
                .andExpect(jsonPath("$.error").value("Monto no permitido"));

        assertThat(PETICIONES_SEND_TX).isEmpty();
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Producto de monto variable dentro del rango: aprobado")
    void montoVariable() throws Exception {
        pagar(UUID.randomUUID().toString(), solicitud("5", "500", "123456789012", "345.50"), 201);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Sin tarjeta: 422 Tarjeta requerida y no se llama al proveedor (ya no se paga con saldo)")
    void sinTarjeta() throws Exception {
        pagar(UUID.randomUUID().toString(), solicitudSinTarjeta(numeroCuenta, "1", "100", "5512345678", "100.00"), 422)
                .andExpect(jsonPath("$.error").value("Tarjeta requerida"));
        assertThat(PETICIONES_SEND_TX).isEmpty();
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Cuenta de otro cliente: 403 (igual que una cuenta inexistente)")
    void cuentaAjena() throws Exception {
        Cliente otro = crearCliente(new BigDecimal("1000.00"), true, "ACTIVA");
        pagar(UUID.randomUUID().toString(), solicitudCuenta(otro.getCuenta().getNumeroCuenta()), 403);
        pagar(UUID.randomUUID().toString(), solicitudCuenta("9999999999"), 403);
        assertThat(cuentaRepository.findByNumeroCuenta(otro.getCuenta().getNumeroCuenta()).orElseThrow().getSaldo())
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Cuenta inactiva: 422; cliente dado de baja: 403")
    void cuentaInactivaYClienteInactivo() throws Exception {
        Cliente inactiva = crearCliente(new BigDecimal("1000.00"), true, "INACTIVA");
        token = "Bearer " + tokenClienteService.emitir(inactiva.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        pagar(UUID.randomUUID().toString(), solicitudCuenta(inactiva.getCuenta().getNumeroCuenta()), 422)
                .andExpect(jsonPath("$.error").value("Cuenta no operable"));

        Cliente baja = crearCliente(new BigDecimal("1000.00"), false, "ACTIVA");
        token = "Bearer " + tokenClienteService.emitir(baja.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        pagar(UUID.randomUUID().toString(), solicitudCuenta(baja.getCuenta().getNumeroCuenta()), 403);
    }

    @Test
    @DisplayName("Sin token, token alterado o sin permiso: 401/403 sin tocar el saldo")
    void autenticacion() throws Exception {
        String body = solicitud("1", "100", "5512345678", "100.00");
        mockMvc.perform(post(URL).header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));

        token = token.substring(0, token.length() - 3) + "abc";
        pagar(UUID.randomUUID().toString(), body, 401);

        token = "Bearer " + tokenClienteService.emitir(cliente.getId(), List.of("otro:permiso"), Duration.ofMinutes(5));
        pagar(UUID.randomUUID().toString(), body, 403);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Sin Idempotency-Key, llave inválida o campos extra: 400")
    void validacionesDeEntrada() throws Exception {
        String body = solicitud("1", "100", "5512345678", "100.00");
        mockMvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        pagar("no-es-uuid", body, 400);
        pagar(UUID.randomUUID().toString(), body.replace("}", ",\"estatus\":\"APROBADA\"}"), 400);
        pagar(UUID.randomUUID().toString(), solicitud("1", "100", "55 12", "-5"), 400)
                .andExpect(jsonPath("$.detalles.monto").exists())
                .andExpect(jsonPath("$.detalles.referencia").exists());
    }

    @Test
    @DisplayName("Cuenta sin saldo: el pago con tarjeta de débito se aplica igual (el saldo no interviene)")
    void sinSaldoPagaConTarjeta() throws Exception {
        Cliente sinSaldo = crearCliente(new BigDecimal("0.00"), true, "ACTIVA");
        token = "Bearer " + tokenClienteService.emitir(sinSaldo.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        String cuenta = sinSaldo.getCuenta().getNumeroCuenta();

        List<Integer> estados = enParalelo(
                () -> pagarStatus(UUID.randomUUID().toString(), solicitud(cuenta, "1", "100", "5511111111", "100.00")),
                () -> pagarStatus(UUID.randomUUID().toString(), solicitud(cuenta, "1", "100", "5522222222", "100.00")));

        assertThat(estados).containsExactly(201, 201);
        assertThat(cuentaRepository.findByNumeroCuenta(cuenta).orElseThrow().getSaldo()).isEqualByComparingTo("0.00");
        assertThat(PETICIONES_SEND_TX).hasSize(2);
    }

    @Test
    @DisplayName("Concurrencia: la misma llave enviada dos veces a la vez solo cobra y envía una vez")
    void concurrenciaMismaLlave() throws Exception {
        String llave = UUID.randomUUID().toString();
        String body = solicitud("1", "100", "5512345678", "100.00");
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(200, "<RESPONSE><CODIGO>0</CODIGO></RESPONSE>", 200));

        List<Integer> estados = enParalelo(() -> pagarStatus(llave, body), () -> pagarStatus(llave, body));

        assertThat(estados).containsExactlyInAnyOrder(201, 200);
        assertThat(PETICIONES_SEND_TX).hasSize(1);
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Consulta por folio: solo el dueño del pago puede verlo")
    void consultaPorFolio() throws Exception {
        String folio = folio(pagar(UUID.randomUUID().toString(), solicitud("1", "100", "5512345678", "100.00"), 201).andReturn());
        String consulta = "{\"folio\":\"" + folio + "\"}";
        mockMvc.perform(post(URL + "/consultar").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(consulta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estatus").value("APROBADA"));

        Cliente otro = crearCliente(new BigDecimal("10.00"), true, "ACTIVA");
        String tokenOtro = "Bearer " + tokenClienteService.emitir(otro.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        mockMvc.perform(post(URL + "/consultar").header("Authorization", tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON).content(consulta))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Pago con tarjeta de débito: no toca el saldo y el comprobante solo muestra los últimos 4 dígitos")
    void pagoConTarjetaDebito() throws Exception {
        String folio = folio(pagar(UUID.randomUUID().toString(), conTarjeta(solicitud("1", "100", "5512345678", "100.00"),
                "4000 1234 5675 9010"), 201)
                .andExpect(jsonPath("$.datos.estatus").value("APROBADA"))
                .andExpect(jsonPath("$.datos.metodoPago").value("TARJETA_DEBITO"))
                .andExpect(jsonPath("$.datos.tarjeta").value("VISA •••• 9010"))
                .andExpect(jsonPath("$.datos.autorizacion").isNotEmpty())
                .andReturn());

        assertThat(saldo()).isEqualByComparingTo("1000.00");
        PagoServicio pago = pagoRepository.findByFolio(folio).orElseThrow();
        assertThat(pago.getTarjetaUltimos4()).isEqualTo("9010");
        assertThat(movimientoRepository.findByPagoIdOrderByIdAsc(pago.getId())).isEmpty();
    }

    @Test
    @DisplayName("Tarjeta de crédito o declinada por el banco: no se registra el pago ni se envía al proveedor")
    void tarjetaCreditoODeclinada() throws Exception {
        pagar(UUID.randomUUID().toString(), conTarjeta(solicitud("1", "100", "5512345678", "100.00"), "4111111111111111"), 422)
                .andExpect(jsonPath("$.error").value("Tarjeta inválida"));
        pagar(UUID.randomUUID().toString(), conTarjeta(solicitud("1", "100", "5512345678", "100.00"), "4000123456740002"), 402);

        assertThat(PETICIONES_SEND_TX).isEmpty();
        assertThat(pagoRepository.findAll()).noneMatch(p -> p.getClienteId().equals(cliente.getId()));
    }

    @Test
    @DisplayName("Proveedor rechaza un pago con tarjeta: queda RECHAZADA, se reembolsa a la tarjeta y el saldo no cambia")
    void rechazoConTarjeta() throws Exception {
        RESPUESTA_SEND_TX.set(new RespuestaSimulada(200, "{\"codigo\":\"05\",\"mensaje\":\"Referencia invalida\"}", 0));

        pagar(UUID.randomUUID().toString(), conTarjeta(solicitud("1", "100", "5512345678", "100.00"), "5200123456754321"), 422)
                .andExpect(jsonPath("$.datos.estatus").value("RECHAZADA"));
        assertThat(saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Tarjetas guardadas: se guardan solo marca/terminación/vencimiento, se paga con el token + CVV y se eliminan")
    void tarjetasGuardadas() throws Exception {
        String nueva = """
                {"numero":"5200 1234 5675 4321","titular":"Angel Perez Lopez","mesVencimiento":11,"anioVencimiento":2029,"cvv":"321"}
                """;
        MvcResult creada = mockMvc.perform(post("/api/tarjetas").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(nueva))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.marca").value("MASTERCARD"))
                .andExpect(jsonPath("$.datos.ultimos4").value("4321"))
                .andExpect(jsonPath("$.datos.vencimiento").value("11/29"))
                .andReturn();
        String cuerpo = creada.getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("5200123456754321").doesNotContain("cvv");
        String id = objectMapper.readTree(cuerpo).path("datos").path("id").asText();
        assertThat(id).matches("tok_[a-f0-9]{32}");
        assertThat(tarjetaGuardadaRepository.findAll()).allSatisfy(t -> assertThat(t.toString()).doesNotContain("5200123456754321"));

        mockMvc.perform(get("/api/tarjetas").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.length()").value(1))
                .andExpect(jsonPath("$.datos[0].id").value(id));

        String conGuardada = agregar(solicitudSinTarjeta(numeroCuenta, "1", "100", "5512345678", "100.00"),
                "\"tarjetaGuardadaId\":\"" + id + "\",\"cvv\":\"321\"");
        pagar(UUID.randomUUID().toString(), conGuardada, 201)
                .andExpect(jsonPath("$.datos.tarjeta").value("MASTERCARD •••• 4321"));

        String sinCvv = agregar(solicitudSinTarjeta(numeroCuenta, "1", "100", "5512345679", "100.00"),
                "\"tarjetaGuardadaId\":\"" + id + "\"");
        pagar(UUID.randomUUID().toString(), sinCvv, 422).andExpect(jsonPath("$.error").value("Tarjeta inválida"));

        // Otro cliente no puede usar ni borrar la tarjeta
        Cliente otro = crearCliente(new BigDecimal("0.00"), true, "ACTIVA");
        String tokenOtro = "Bearer " + tokenClienteService.emitir(otro.getId(), List.of(ClienteAutenticado.PERMISO_PAGAR_SERVICIOS), Duration.ofMinutes(5));
        mockMvc.perform(delete("/api/tarjetas/" + id).header("Authorization", tokenOtro)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/tarjetas").header("Authorization", tokenOtro)).andExpect(jsonPath("$.datos.length()").value(0));

        mockMvc.perform(delete("/api/tarjetas/" + id).header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/tarjetas").header("Authorization", token)).andExpect(jsonPath("$.datos.length()").value(0));
        pagar(UUID.randomUUID().toString(), agregar(solicitudSinTarjeta(numeroCuenta, "1", "100", "5512345670", "100.00"),
                "\"tarjetaGuardadaId\":\"" + id + "\",\"cvv\":\"321\""), 404);
    }

    @Test
    @DisplayName("Pagar con una tarjeta nueva marcando 'guardar': la tarjeta queda guardada para el siguiente pago")
    void pagarYGuardarTarjeta() throws Exception {
        pagar(UUID.randomUUID().toString(), agregar(solicitud("1", "100", "5512345678", "100.00"), "\"guardarTarjeta\":true"), 201);
        pagar(UUID.randomUUID().toString(), agregar(solicitud("1", "100", "5512345679", "100.00"), "\"guardarTarjeta\":true"), 201);

        mockMvc.perform(get("/api/tarjetas").header("Authorization", token))
                .andExpect(jsonPath("$.datos.length()").value(1))
                .andExpect(jsonPath("$.datos[0].ultimos4").value("9010"))
                .andExpect(jsonPath("$.datos[0].marca").value("VISA"));
        mockMvc.perform(get("/api/tarjetas")).andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private org.springframework.test.web.servlet.ResultActions pagar(String llave, String body, int esperado) throws Exception {
        return mockMvc.perform(post(URL)
                        .header("Authorization", token)
                        .header("Idempotency-Key", llave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(esperado));
    }

    private int pagarStatus(String llave, String body) throws Exception {
        return mockMvc.perform(post(URL)
                        .header("Authorization", token)
                        .header("Idempotency-Key", llave)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    @SafeVarargs
    private List<Integer> enParalelo(Callable<Integer>... tareas) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tareas.length);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> tarea : tareas) {
                futuros.add(executor.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                resultados.add(f.get());
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private String folio(MvcResult resultado) throws Exception {
        JsonNode json = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return json.path("datos").path("folio").asText();
    }

    private BigDecimal saldo() {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta).orElseThrow().getSaldo();
    }

    private String solicitud(String idServicio, String idProducto, String referencia, String monto) {
        return solicitud(numeroCuenta, idServicio, idProducto, referencia, monto);
    }

    static final String TARJETA_DEBITO = "4000123456759010";

    private static String conTarjeta(String solicitud, String numero) {
        return solicitud.replace(TARJETA_DEBITO, numero);
    }

    private static String agregar(String solicitud, String json) {
        return solicitud.strip().replaceFirst("}$", "," + json + "}");
    }

    private String solicitudCuenta(String cuenta) {
        return solicitud(cuenta, "1", "100", "5512345678", "100.00");
    }

    /** Solicitud con una tarjeta de débito nueva (sin guardarla). */
    private String solicitud(String cuenta, String idServicio, String idProducto, String referencia, String monto) {
        return agregar(solicitudSinTarjeta(cuenta, idServicio, idProducto, referencia, monto), """
                "tarjeta":{"numero":"%s","titular":"Angel Perez Lopez","mesVencimiento":12,"anioVencimiento":2030,"cvv":"123"}
                """.formatted(TARJETA_DEBITO).strip());
    }

    private String solicitudSinTarjeta(String cuenta, String idServicio, String idProducto, String referencia, String monto) {
        return """
                {"numeroCuenta":"%s","idServicio":"%s","idProducto":"%s","referencia":"%s","validacionId":"%s","monto":%s}
                """.formatted(cuenta, idServicio, idProducto, referencia, validacion(idServicio, idProducto, referencia), monto);
    }

    private Long clienteDelToken() {
        try {
            return tokenClienteService.autenticar(token).clienteId();
        } catch (RuntimeException e) {
            return cliente.getId();
        }
    }

    /** Comprobante de validación vigente (el flujo HTTP de validación se prueba en ValidacionReferenciaIntegrationTest). */
    private String validacion(String idServicio, String idProducto, String referencia) {
        ValidacionReferencia v = new ValidacionReferencia();
        v.setId(UUID.randomUUID().toString());
        v.setClienteId(clienteDelToken());
        v.setIdServicio(idServicio);
        v.setIdProducto(idProducto);
        v.setReferencia(referencia.length() > 40 ? referencia.substring(0, 40) : referencia);
        v.setCreatedAt(LocalDateTime.now());
        v.setExpiraEn(LocalDateTime.now().plusMinutes(10));
        validacionRepository.save(v);
        return v.getId();
    }

    private Cliente crearCliente(BigDecimal saldo, boolean activo, String estatusCuenta) {
        int n = SECUENCIA.incrementAndGet();
        String sufijo = String.format("%02d", n % 100);
        String unico = UUID.randomUUID().toString().replace("-", "").substring(0, 7).toUpperCase();
        Cliente c = new Cliente();
        c.setNombre("Test");
        c.setApellidoPaterno("Apellido");
        c.setApellidoMaterno("Materno");
        c.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        c.setCurp(("PAGO" + unico + "HDFBB" + sufijo).substring(0, 18));
        c.setRfc(("PG" + unico + sufijo + "AB1").substring(0, 13));
        c.setSexo("M");
        c.setNacionalidad("Mexicana");
        c.setEstadoCivil("Soltero");
        c.setCorreo("pago" + unico + "@test.com");
        c.setTelefonoMovil("3300000000");
        c.setOcupacion("QA");
        c.setEmpresa("TestCo");
        c.setIngresoMensual(BigDecimal.valueOf(10000));
        c.setActivo(activo);
        c.setCreatedAt(LocalDateTime.now());

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(String.format("7%011d", BASE_CUENTAS + n));
        cuenta.setSaldo(saldo);
        cuenta.setEstatus(estatusCuenta);
        cuenta.setCliente(c);
        c.setCuenta(cuenta);
        return clienteRepository.save(c);
    }

    // ─────────────────────────────────────────────
    //  Gestopago simulado
    // ─────────────────────────────────────────────

    private record RespuestaSimulada(int status, String cuerpo, long retrasoMs) {
    }

    private static void sendTx(HttpExchange exchange) throws IOException {
        AUTHORIZATION_RECIBIDOS.add(exchange.getRequestHeaders().getFirst("Authorization"));
        PETICIONES_SEND_TX.add(formulario(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
        RespuestaSimulada respuesta = RESPUESTA_SEND_TX.get();
        if (respuesta.retrasoMs() > 0) {
            try {
                Thread.sleep(respuesta.retrasoMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        responder(exchange, respuesta.status(), respuesta.cuerpo());
    }

    private static Map<String, String> formulario(String cuerpo) {
        Map<String, String> parametros = new HashMap<>();
        for (String par : cuerpo.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2) {
                parametros.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return parametros;
    }

    private static void responder(HttpExchange exchange, int status, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            // El cliente cerró la conexión (timeout simulado)
        } finally {
            exchange.close();
        }
    }
}
