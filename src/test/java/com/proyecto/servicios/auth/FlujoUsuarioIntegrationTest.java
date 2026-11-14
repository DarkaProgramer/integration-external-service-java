package com.proyecto.servicios.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo que usan la web y la app: registro → login en otro dispositivo (alerta) → catálogo →
 * validar referencia con Gestopago → pagar → historial y detalle → logout. Gestopago se simula con un servidor HTTP local.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Flujo de usuario: sesiones, catálogo, validación, pago e historial")
class FlujoUsuarioIntegrationTest {

    private static final String CATALOGO_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RESPONSE>
              <MENSAJE><CODIGO>0</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
              <PRODUCTOS>
                <producto servicio="Telcel" producto="Telcel $100" idServicio="1" idProducto="100" precio="100.00"
                          tipoReferencia="NUMERO" longitudReferencia="10" idCatTipoServicio="1"/>
                <producto servicio="CFE" producto="Pago CFE" idServicio="5" idProducto="500" precio="0"
                          categoria="Luz" logo="https://cdn.ejemplo.com/cfe.png" descripcion="Recibo de luz"/>
              </PRODUCTOS>
            </RESPONSE>
            """;
    private static final String PASSWORD = "Secreta123";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final HttpServer GESTOPAGO;
    private static final AtomicReference<String> RESPUESTA_REFERENCIA = new AtomicReference<>();

    static {
        try {
            GESTOPAGO = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            GESTOPAGO.setExecutor(Executors.newCachedThreadPool());
            GESTOPAGO.createContext("/sistema/service/getProductList.do", ex -> responder(ex, CATALOGO_XML));
            GESTOPAGO.createContext("/sistema/service/consultaReferencia.do", ex -> responder(ex, RESPUESTA_REFERENCIA.get()));
            GESTOPAGO.createContext("/sistema/service/sendTx.do",
                    ex -> responder(ex, "<RESPONSE><CODIGO>0</CODIGO><TRANSACCION>GP-123</TRANSACCION></RESPONSE>"));
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
        registry.add("gestopago.referencia.url", () -> url);
        registry.add("gestopago.referencia.path", () -> "/sistema/service/consultaReferencia.do");
        registry.add("gestopago.catalogo.categorias.1", () -> "Recargas");
        registry.add("app.seguridad.login.max-intentos", () -> "3");
    }

    @AfterAll
    static void detener() {
        GESTOPAGO.stop(0);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String correo;

    @BeforeEach
    void setUp() {
        correo = "usuario" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        RESPUESTA_REFERENCIA.set("<RESPONSE><CODIGO>0</CODIGO><MONTO>345.50</MONTO></RESPONSE>");
    }

    @Test
    @DisplayName("Registro en web, login en móvil: la web recibe la alerta y puede cerrar la sesión del móvil")
    void sesionesSimultaneasConAlerta() throws Exception {
        JsonNode web = datos(registrar("WEB", "Chrome").andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.datos.perfil.correo").value(correo))
                .andExpect(jsonPath("$.datos.perfil.numeroCuenta").exists())
                .andExpect(jsonPath("$.datos.otrasSesionesActivas").isEmpty()));
        String tokenWeb = web.get("accessToken").asText();

        // La web todavía no tiene alertas
        conToken(get("/api/auth/alertas"), tokenWeb).andExpect(jsonPath("$.datos").isEmpty());

        JsonNode movil = datos(login(correo, PASSWORD, "MOVIL", "Pixel 8").andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.otrasSesionesActivas.length()").value(1))
                .andExpect(jsonPath("$.datos.otrasSesionesActivas[0].tipoDispositivo").value("WEB")));
        String tokenMovil = movil.get("accessToken").asText();

        // Ambas sesiones siguen activas a la vez
        conToken(get("/api/auth/perfil"), tokenWeb).andExpect(status().isOk());
        conToken(get("/api/auth/perfil"), tokenMovil).andExpect(status().isOk());
        conToken(get("/api/auth/sesiones"), tokenMovil).andExpect(jsonPath("$.datos.length()").value(2));

        // La web recibe el aviso del nuevo dispositivo
        conToken(get("/api/auth/alertas"), tokenWeb)
                .andExpect(jsonPath("$.datos.length()").value(1))
                .andExpect(jsonPath("$.datos[0].tipoDispositivo").value("MOVIL"))
                .andExpect(jsonPath("$.datos[0].nombreDispositivo").value("Pixel 8"))
                .andExpect(jsonPath("$.datos[0].sesionId").value(movil.get("sesionId").asText()));
        conToken(post("/api/auth/alertas/leidas"), tokenWeb).andExpect(jsonPath("$.datos.marcadas").value(1));
        conToken(get("/api/auth/alertas"), tokenWeb).andExpect(jsonPath("$.datos").isEmpty());
        // El móvil (sesión nueva) no recibe alerta de sí mismo
        conToken(get("/api/auth/alertas"), tokenMovil).andExpect(jsonPath("$.datos").isEmpty());

        // "No fui yo": la web cierra la sesión del móvil y su token deja de servir de inmediato
        conToken(post("/api/auth/sesiones/cerrar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sesionId\":\"" + movil.get("sesionId").asText() + "\"}"), tokenWeb)
                .andExpect(status().isOk());
        conToken(get("/api/auth/perfil"), tokenMovil).andExpect(status().isUnauthorized());
        refrescar(movil.get("refreshToken").asText()).andExpect(status().isUnauthorized());
        conToken(get("/api/auth/perfil"), tokenWeb).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Refresh rota el token; logout invalida access y refresh")
    void refreshYLogout() throws Exception {
        JsonNode sesion = datos(registrar("MOVIL", null));
        String refresh1 = sesion.get("refreshToken").asText();

        JsonNode renovada = datos(refrescar(refresh1).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.sesionId").value(sesion.get("sesionId").asText())));
        String refresh2 = renovada.get("refreshToken").asText();
        assertThat(refresh2).isNotEqualTo(refresh1);
        refrescar(refresh1).andExpect(status().isUnauthorized());

        String access = renovada.get("accessToken").asText();
        conToken(post("/api/auth/logout"), access).andExpect(status().isOk());
        conToken(get("/api/auth/perfil"), access).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        refrescar(refresh2).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login: credenciales inválidas 401 con mensaje genérico; bloqueo tras varios intentos")
    void loginInvalidoYBloqueo() throws Exception {
        registrar("WEB", null);
        login("noexiste@test.com", PASSWORD, "WEB", null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        login(correo, "Incorrecta1", "WEB", null).andExpect(status().isUnauthorized());
        login(correo, "Incorrecta2", "WEB", null).andExpect(status().isUnauthorized());
        login(correo, "Incorrecta3", "WEB", null).andExpect(status().isUnauthorized());
        // Bloqueado aunque ahora la contraseña sea correcta
        login(correo, PASSWORD, "WEB", null).andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Registro: contraseña débil 400 y correo repetido 409")
    void registroInvalido() throws Exception {
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("WEB", null, "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.password").exists());
        registrar("WEB", null).andExpect(status().isCreated());
        registrar("WEB", null).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Catálogo en vivo, validación de referencia, pago, historial y detalle")
    void catalogoValidacionPagoEHistorial() throws Exception {
        JsonNode sesion = datos(registrar("WEB", null));
        String token = sesion.get("accessToken").asText();
        String cuenta = sesion.get("perfil").get("numeroCuenta").asText();

        conToken(get("/api/catalogo/productos"), token).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[0].categoria").value("Luz"))
                .andExpect(jsonPath("$.datos[0].logoUrl").value("https://cdn.ejemplo.com/cfe.png"))
                .andExpect(jsonPath("$.datos[0].montoVariable").value(true))
                .andExpect(jsonPath("$.datos[1].identificador").value("1-100"))
                .andExpect(jsonPath("$.datos[1].categoria").value("Recargas"))
                .andExpect(jsonPath("$.datos[1].servicio").value("Telcel"))
                .andExpect(jsonPath("$.datos[1].precio").value(100.00))
                .andExpect(jsonPath("$.datos[1].referencia.soloDigitos").value(true))
                .andExpect(jsonPath("$.datos[1].referencia.longitudMaxima").value(10));
        mockMvc.perform(get("/api/catalogo/productos")).andExpect(status().isUnauthorized());

        // Formato inválido para el producto: ni siquiera se consulta al proveedor
        validar(token, "1", "100", "55ABC").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("La referencia de este servicio solo admite dígitos"));
        // El proveedor no reconoce la referencia
        RESPUESTA_REFERENCIA.set("<RESPONSE><CODIGO>12</CODIGO><TEXTO>No existe</TEXTO></RESPONSE>");
        validar(token, "1", "100", "5512345678").andExpect(status().isUnprocessableEntity());
        // Proveedor caído / respuesta ilegible: 503
        RESPUESTA_REFERENCIA.set("no es xml");
        validar(token, "1", "100", "5512345678").andExpect(status().isServiceUnavailable());

        RESPUESTA_REFERENCIA.set("<RESPONSE><CODIGO>0</CODIGO></RESPONSE>");
        String validacionId = datos(validar(token, "1", "100", "5512345678").andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.referencia").value("******5678"))
                .andExpect(jsonPath("$.datos.precio").value(100.00)))
                .get("validacionId").asText();

        // Sin validación (o con la de otra referencia) no se paga
        pagar(token, cuenta, "5599999999", UUID.randomUUID().toString()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Referencia no validada"));

        String folio = datos(pagar(token, cuenta, "5512345678", validacionId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.resultado").value("EXITOSO"))
                .andExpect(jsonPath("$.datos.servicio").value("Telcel"))
                .andExpect(jsonPath("$.datos.folioProveedor").value("GP-123")))
                .get("folio").asText();

        // La misma validación no sirve para un segundo pago
        pagar(token, cuenta, "5512345678", validacionId).andExpect(status().isConflict());

        conToken(post("/api/pagos/servicios/historial"), token).andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.totalElementos").value(1))
                .andExpect(jsonPath("$.datos.contenido[0].folio").value(folio))
                .andExpect(jsonPath("$.datos.contenido[0].mensaje").value("Pago realizado correctamente"));
        conToken(post("/api/pagos/servicios/historial").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pagina\":0,\"tamano\":5,\"resultado\":\"RECHAZADO\"}"), token)
                .andExpect(jsonPath("$.datos.totalElementos").value(0));
        conToken(post("/api/pagos/servicios/consultar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"folio\":\"" + folio + "\"}"), token)
                .andExpect(jsonPath("$.datos.estatus").value("APROBADA"));
        conToken(get("/api/auth/perfil"), token).andExpect(jsonPath("$.datos.saldo").value(900.00));

        // Otro usuario no ve el historial ni el detalle ajeno
        correo = "otro" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String tokenOtro = datos(registrar("MOVIL", null)).get("accessToken").asText();
        conToken(post("/api/pagos/servicios/historial"), tokenOtro).andExpect(jsonPath("$.datos.totalElementos").value(0));
        conToken(post("/api/pagos/servicios/consultar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"folio\":\"" + folio + "\"}"), tokenOtro).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CORS: solo el origen configurado del frontend web")
    void cors() throws Exception {
        mockMvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mockMvc.perform(options("/api/auth/login").header("Origin", "https://malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    // ───────────── utilidades ─────────────

    private ResultActions registrar(String tipo, String nombre) throws Exception {
        return mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(registroJson(tipo, nombre, PASSWORD)));
    }

    private ResultActions login(String correoLogin, String password, String tipo, String nombre) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(java.util.Map.of("correo", correoLogin, "password", password,
                        "dispositivo", dispositivo(tipo, nombre)))));
    }

    private ResultActions refrescar(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private ResultActions validar(String token, String idServicio, String idProducto, String referencia) throws Exception {
        return conToken(post("/api/pagos/referencias/validar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idServicio\":\"%s\",\"idProducto\":\"%s\",\"referencia\":\"%s\"}"
                        .formatted(idServicio, idProducto, referencia)), token);
    }

    private ResultActions pagar(String token, String cuenta, String referencia, String validacionId) throws Exception {
        return conToken(post("/api/pagos/servicios").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"numeroCuenta":"%s","idServicio":"1","idProducto":"100","referencia":"%s","validacionId":"%s","monto":100.00}
                        """.formatted(cuenta, referencia, validacionId)), token);
    }

    private ResultActions conToken(MockHttpServletRequestBuilder peticion, String token) throws Exception {
        return mockMvc.perform(peticion.header("Authorization", "Bearer " + token));
    }

    private JsonNode datos(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8)).get("datos");
    }

    private java.util.Map<String, Object> dispositivo(String tipo, String nombre) {
        java.util.Map<String, Object> d = new java.util.HashMap<>();
        d.put("tipo", tipo);
        if (nombre != null) {
            d.put("nombre", nombre);
        }
        return d;
    }

    private String registroJson(String tipo, String nombre, String password) throws Exception {
        String letras = letras(4);
        String curp = letras + "900515H" + letras(5) + "09";
        String rfc = letras + "900515AB1";
        java.util.Map<String, Object> domicilio = java.util.Map.of("calle", "Reforma", "numeroExterior", "10",
                "colonia", "Centro", "municipio", "Guadalajara", "estado", "Jalisco", "codigoPostal", "44100", "pais", "México");
        java.util.Map<String, Object> cuerpo = new java.util.HashMap<>();
        cuerpo.put("nombre", "Ana");
        cuerpo.put("apellidoPaterno", "López");
        cuerpo.put("apellidoMaterno", "García");
        cuerpo.put("fechaNacimiento", "1990-05-15");
        cuerpo.put("curp", curp);
        cuerpo.put("rfc", rfc);
        cuerpo.put("sexo", "F");
        cuerpo.put("nacionalidad", "Mexicana");
        cuerpo.put("estadoCivil", "Soltera");
        cuerpo.put("correo", correo);
        cuerpo.put("telefonoMovil", "3312345678");
        cuerpo.put("domicilio", domicilio);
        cuerpo.put("ocupacion", "Ingeniera");
        cuerpo.put("empresa", "Hasani");
        cuerpo.put("ingresoMensual", 25000);
        cuerpo.put("password", password);
        cuerpo.put("dispositivo", dispositivo(tipo, nombre));
        return objectMapper.writeValueAsString(cuerpo);
    }

    private static String letras(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append((char) ('A' + RANDOM.nextInt(26)));
        }
        return sb.toString();
    }

    private static void responder(HttpExchange exchange, String cuerpo) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        } finally {
            exchange.close();
        }
    }
}
