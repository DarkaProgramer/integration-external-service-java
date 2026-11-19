package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.config.GestoPagoBearerToken;
import com.proyecto.servicios.config.GestoPagoCatalogoProperties;
import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.exception.pagos.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.pagos.MontoNoPermitidoException;
import com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ReglaReferencia;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Valida servicio, producto y monto contra el catálogo real de Gestopago (getProductList.do).
 * <ul>
 *     <li>El catálogo se guarda en memoria durante {@code pagos.catalogo.ttl-minutos} para no consultar al
 *     proveedor en cada pago.</li>
 *     <li>Si al vencer la copia el catálogo no se puede obtener, se falla cerrado (503): nunca se procesa un
 *     pago sin validar el producto.</li>
 *     <li>El precio y el nombre salen del catálogo; el monto del cliente solo se acepta si coincide.</li>
 * </ul>
 */
@Slf4j
@Service
public class GestoPagoCatalogoProductosService implements CatalogoProductosService {

    private final GestoPagoProductsClient productsClient;
    private final PagosProperties properties;
    private final Clock clock;
    private final GestoPagoCatalogoProperties presentacion;

    /** Opcional: permite pedir un token nuevo a Gestopago si el actual fue rechazado (401/403). */
    private GestoPagoBearerToken bearerToken;

    private volatile Catalogo catalogo = new Catalogo(Collections.emptyMap(), Instant.EPOCH);

    public GestoPagoCatalogoProductosService(GestoPagoProductsClient productsClient, PagosProperties properties, Clock clock) {
        this(productsClient, properties, clock, new GestoPagoCatalogoProperties());
    }

    @Autowired
    public GestoPagoCatalogoProductosService(GestoPagoProductsClient productsClient, PagosProperties properties, Clock clock,
                                             GestoPagoCatalogoProperties presentacion) {
        this.productsClient = productsClient;
        this.properties = properties;
        this.clock = clock;
        this.presentacion = presentacion;
    }

    @Autowired(required = false)
    void setBearerToken(GestoPagoBearerToken bearerToken) {
        this.bearerToken = bearerToken;
    }

    @Override
    public ProductoCatalogo validarProductoYMonto(String idServicio, String idProducto, BigDecimal monto) {
        ProductoCatalogo producto = obtener(idServicio, idProducto);
        validarMonto(producto, monto);
        return producto;
    }

    @Override
    public ProductoCatalogo obtener(String idServicio, String idProducto) {
        return Optional.ofNullable(productosVigentes().get(llave(idServicio, idProducto)))
                .orElseThrow(ProductoNoDisponibleException::new);
    }

    @Override
    public List<ProductoCatalogo> listar() {
        return productosVigentes().values().stream()
                .sorted(Comparator.comparing((ProductoCatalogo p) -> textoOrden(p.categoria()))
                        .thenComparing(p -> textoOrden(p.servicio()))
                        .thenComparing(p -> textoOrden(p.nombre())))
                .toList();
    }

    private static String textoOrden(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT);
    }

    private void validarMonto(ProductoCatalogo producto, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new MontoNoPermitidoException("El monto debe ser mayor a cero y con máximo 2 decimales");
        }
        if (producto.tienePrecioFijo()) {
            if (monto.compareTo(producto.precio()) != 0) {
                throw new MontoNoPermitidoException("El monto no corresponde al precio del producto seleccionado");
            }
            return;
        }
        if (monto.compareTo(properties.getMontoMinimo()) < 0 || monto.compareTo(properties.getMontoMaximo()) > 0) {
            throw new MontoNoPermitidoException("El monto debe estar entre " + properties.getMontoMinimo().toPlainString()
                    + " y " + properties.getMontoMaximo().toPlainString());
        }
    }

    private Map<String, ProductoCatalogo> productosVigentes() {
        Catalogo actual = catalogo;
        if (actual.vigenteEn(clock.instant())) {
            return actual.productos();
        }
        synchronized (this) {
            actual = catalogo;
            if (actual.vigenteEn(clock.instant())) {
                return actual.productos();
            }
            Map<String, ProductoCatalogo> productos = descargar();
            catalogo = new Catalogo(productos, clock.instant().plus(Duration.ofMinutes(properties.getCatalogo().getTtlMinutos())));
            return productos;
        }
    }

    private Map<String, ProductoCatalogo> descargar() {
        String cuerpo = null;
        try {
            cuerpo = consultarConReintentoDeToken();
            RespuestaGestoPago respuesta = RespuestaGestoPago.leer(cuerpo);
            Map<String, ProductoCatalogo> productos = new HashMap<>();
            respuesta.buscarNodos(n -> n.valor("idProducto").isPresent() && n.valor("idServicio").isPresent())
                    .forEach(n -> aProducto(n).ifPresent(p -> productos.put(llave(p.idServicio(), p.idProducto()), p)));
            if (productos.isEmpty()) {
                log.error("GestoPago respondió sin productos: codigo={}, mensaje={}",
                        recortar(respuesta.buscar("codigo", "code", "codigoRespuesta", "status", "estatus").orElse("-")),
                        recortar(respuesta.buscar("mensaje", "message", "descripcion", "error", "msg").orElse("-")));
                throw new IllegalStateException("catálogo vacío");
            }
            log.info("Catálogo de GestoPago actualizado: productos={}", productos.size());
            return Collections.unmodifiableMap(productos);
        } catch (RuntimeException e) {
            log.error("No fue posible obtener el catálogo de GestoPago: causa={}, detalle={}", e.getClass().getSimpleName(), diagnostico(e));
            if (e instanceof RespuestaGestoPago.RespuestaIlegibleException) {
                // El catálogo no lleva datos sensibles: el inicio del cuerpo ayuda a saber qué devolvió el proveedor
                log.error("Inicio de la respuesta de GestoPago: {}", recortar(cuerpo));
            }
            throw new CatalogoNoDisponibleException();
        }
    }

    /** Si Gestopago rechaza el token (401/403), pide uno nuevo y reintenta una sola vez (consulta de solo lectura). */
    private String consultarConReintentoDeToken() {
        try {
            return productsClient.getProductListRaw();
        } catch (FeignException e) {
            if ((e.status() == 401 || e.status() == 403) && bearerToken != null) {
                log.warn("GestoPago rechazó el token del catálogo (HTTP {}); se solicita uno nuevo", e.status());
                bearerToken.renovar();
                return productsClient.getProductListRaw();
            }
            throw e;
        }
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return "(vacía)";
        }
        String limpio = texto.replaceAll("\\s+", " ").strip();
        return limpio.length() > 200 ? limpio.substring(0, 200) + "..." : limpio;
    }

    /** Pista para los logs de Render sin exponer token, URL ni cuerpo de la respuesta. */
    private static String diagnostico(RuntimeException e) {
        if (e instanceof GestoPagoIntegrationException g) {
            return "tipo=" + g.getErrorType() + " (falta GESTOPAGO_PASSWORD o GESTOPAGO_PRODUCTS_TOKEN)";
        }
        if (e instanceof FeignException f) {
            if (f.status() == 401 || f.status() == 403) {
                return "HTTP " + f.status() + ": token rechazado; revisar GESTOPAGO_PASSWORD y que GESTOPAGO_URL sea del mismo ambiente";
            }
            if (f.status() == 404) {
                return "HTTP 404: revisar GESTOPAGO_URL (debe ser la URL base, sin /sistema/...)";
            }
            return f.status() < 0 ? "sin conexión o timeout con GESTOPAGO_URL" : "HTTP " + f.status();
        }
        if (e instanceof RespuestaGestoPago.RespuestaIlegibleException) {
            return "Gestopago respondió algo que no es XML ni JSON (¿GESTOPAGO_URL apunta a una página web?)";
        }
        if (e instanceof IllegalStateException) {
            return "Gestopago respondió pero sin productos reconocibles";
        }
        return "error al interpretar la respuesta";
    }

    private Optional<ProductoCatalogo> aProducto(RespuestaGestoPago nodo) {
        String idServicio = nodo.valor("idServicio").orElseThrow();
        String idProducto = nodo.valor("idProducto").orElseThrow();
        String nombre = nodo.valor("producto", "nombre", "name", "nombreProducto").orElse(null);
        BigDecimal precio;
        try {
            precio = nodo.valor("precio", "price").map(BigDecimal::new).orElse(null);
        } catch (NumberFormatException e) {
            // Un precio ilegible no puede validar montos: se descarta el producto (falla cerrada)
            return Optional.empty();
        }
        String servicio = nodo.valor("servicio", "nombreServicio", "proveedor", "marca").orElse(null);
        String descripcion = nodo.valor("descripcion", "description", "leyenda", "legend", "ayuda")
                .orElseGet(() -> descripcionPorDefecto(servicio, nombre));
        return Optional.of(new ProductoCatalogo(idServicio, idProducto, nombre, precio, servicio, descripcion,
                categoria(nodo), logo(nodo, idServicio), reglaReferencia(nodo)));
    }

    private String categoria(RespuestaGestoPago nodo) {
        Optional<String> nombre = nodo.valor("categoria", "nombreCategoria", "tipoServicio", "catTipoServicio");
        if (nombre.isPresent() && !esNumero(nombre.get())) {
            return nombre.get();
        }
        Optional<String> id = nodo.valor("idCatTipoServicio", "idCategoria", "idTipoServicio").or(() -> nombre);
        return id.map(i -> presentacion.getCategorias().getOrDefault(i, "Categoría " + i)).orElse(CATEGORIA_GENERAL);
    }

    private String logo(RespuestaGestoPago nodo, String idServicio) {
        return nodo.valor("logo", "urlLogo", "logoUrl", "imagen", "urlImagen", "img", "icono")
                .filter(GestoPagoCatalogoProductosService::esUrlSegura)
                .orElseGet(() -> presentacion.getLogoUrl() == null || presentacion.getLogoUrl().isBlank()
                        ? null
                        : presentacion.getLogoUrl().replace("{idServicio}", idServicio));
    }

    /** Longitud y tipo de referencia si el catálogo los informa; si no, la regla general (4 a 40 alfanuméricos). */
    private static ReglaReferencia reglaReferencia(RespuestaGestoPago nodo) {
        Integer maxima = entero(nodo.valor("longitudReferencia", "longitudMaxima", "maxLongitud", "longitud", "maxLength"));
        Integer minima = entero(nodo.valor("longitudMinima", "minLongitud", "minLength"));
        boolean soloDigitos = nodo.valor("tipoReferencia")
                .map(t -> TIPOS_NUMERICOS.contains(t.toUpperCase(Locale.ROOT)))
                .orElse(false);
        int max = maxima == null ? ReglaReferencia.MAXIMO_GENERAL : maxima;
        int min = minima == null ? Math.min(ReglaReferencia.MINIMO_GENERAL, max) : minima;
        return new ReglaReferencia(min, max, soloDigitos);
    }

    private static Integer entero(Optional<String> valor) {
        return valor.filter(GestoPagoCatalogoProductosService::esNumero).map(Integer::valueOf).filter(v -> v > 0).orElse(null);
    }

    private static boolean esNumero(String valor) {
        return valor.length() <= 6 && valor.chars().allMatch(Character::isDigit);
    }

    private static boolean esUrlSegura(String url) {
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private static String descripcionPorDefecto(String servicio, String nombre) {
        if (servicio == null) {
            return nombre;
        }
        return nombre == null || nombre.equalsIgnoreCase(servicio) ? servicio : servicio + " - " + nombre;
    }

    private static final String CATEGORIA_GENERAL = "Servicios";
    /** Valores de tipoReferencia que indican referencia solo numérica (p. ej. número telefónico o de servicio). */
    private static final Set<String> TIPOS_NUMERICOS = Set.of("N", "NUM", "NUMERO", "NUMERICO", "NUMERICA", "TELEFONO");

    private static String llave(String idServicio, String idProducto) {
        return idServicio + "|" + idProducto;
    }

    private record Catalogo(Map<String, ProductoCatalogo> productos, Instant expira) {
        boolean vigenteEn(Instant ahora) {
            return ahora.isBefore(expira);
        }
    }
}
