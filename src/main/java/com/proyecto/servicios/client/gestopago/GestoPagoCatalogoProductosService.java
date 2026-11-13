package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoProductsClient;
import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.exception.pagos.CatalogoNoDisponibleException;
import com.proyecto.servicios.exception.pagos.MontoNoPermitidoException;
import com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private volatile Catalogo catalogo = new Catalogo(Collections.emptyMap(), Instant.EPOCH);

    public GestoPagoCatalogoProductosService(GestoPagoProductsClient productsClient, PagosProperties properties, Clock clock) {
        this.productsClient = productsClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ProductoCatalogo validarProductoYMonto(String idServicio, String idProducto, BigDecimal monto) {
        ProductoCatalogo producto = Optional.ofNullable(productosVigentes().get(llave(idServicio, idProducto)))
                .orElseThrow(ProductoNoDisponibleException::new);
        validarMonto(producto, monto);
        return producto;
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
        try {
            RespuestaGestoPago respuesta = RespuestaGestoPago.leer(productsClient.getProductListRaw());
            Map<String, ProductoCatalogo> productos = new HashMap<>();
            respuesta.buscarNodos(n -> n.valor("idProducto").isPresent() && n.valor("idServicio").isPresent())
                    .forEach(n -> aProducto(n).ifPresent(p -> productos.put(llave(p.idServicio(), p.idProducto()), p)));
            if (productos.isEmpty()) {
                throw new IllegalStateException("catálogo vacío");
            }
            log.info("Catálogo de GestoPago actualizado: productos={}", productos.size());
            return Collections.unmodifiableMap(productos);
        } catch (RuntimeException e) {
            log.error("No fue posible obtener el catálogo de GestoPago: causa={}", e.getClass().getSimpleName());
            throw new CatalogoNoDisponibleException();
        }
    }

    private Optional<ProductoCatalogo> aProducto(RespuestaGestoPago nodo) {
        String idServicio = nodo.valor("idServicio").orElseThrow();
        String idProducto = nodo.valor("idProducto").orElseThrow();
        String nombre = nodo.valor("producto", "nombre", "name").orElse(null);
        BigDecimal precio;
        try {
            precio = nodo.valor("precio", "price").map(BigDecimal::new).orElse(null);
        } catch (NumberFormatException e) {
            // Un precio ilegible no puede validar montos: se descarta el producto (falla cerrada)
            return Optional.empty();
        }
        return Optional.of(new ProductoCatalogo(idServicio, idProducto, nombre, precio));
    }

    private static String llave(String idServicio, String idProducto) {
        return idServicio + "|" + idProducto;
    }

    private record Catalogo(Map<String, ProductoCatalogo> productos, Instant expira) {
        boolean vigenteEn(Instant ahora) {
            return ahora.isBefore(expira);
        }
    }
}
