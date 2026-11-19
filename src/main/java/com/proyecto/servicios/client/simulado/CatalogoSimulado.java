package com.proyecto.servicios.client.simulado;

import com.proyecto.servicios.config.PagosProperties;
import com.proyecto.servicios.exception.pagos.MontoNoPermitidoException;
import com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException;
import com.proyecto.servicios.service.pagos.CatalogoProductosService;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ReglaReferencia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo de servicios para prácticas y demostraciones (solo con {@code gestopago.modo=simulado}; el predeterminado es "real").
 * No llama a Gestopago: siempre responde, así la web y la app se pueden probar completas.
 * Para usar el catálogo real: variable {@code GESTOPAGO_MODO=real}.
 */
@Slf4j
@Primary
@Service
@ConditionalOnProperty(name = "gestopago.modo", havingValue = "simulado")
public class CatalogoSimulado implements CatalogoProductosService {

    private static final ReglaReferencia TELEFONO = new ReglaReferencia(10, 10, true);
    private static final ReglaReferencia RECIBO_CFE = new ReglaReferencia(12, 30, true);
    private static final ReglaReferencia CONTRATO = new ReglaReferencia(6, 20, false);

    private final PagosProperties properties;
    private final Map<String, ProductoCatalogo> productos = new LinkedHashMap<>();

    public CatalogoSimulado(PagosProperties properties) {
        this.properties = properties;
        // Recargas de tiempo aire (precio fijo, referencia = número a 10 dígitos)
        recarga("101", "Telcel", 20, 50, 100, 200, 300, 500);
        recarga("102", "AT&T", 50, 100, 200, 300);
        recarga("103", "Movistar", 50, 100, 200);
        recarga("104", "Bait", 50, 100, 200);
        recarga("105", "Unefon", 50, 100, 200);
        recarga("106", "Virgin Mobile", 50, 100, 200);
        // Pago de recibos (monto variable)
        recibo("201", "CFE", "Luz", "Pago de recibo de luz", RECIBO_CFE);
        recibo("301", "Agua municipal", "Agua", "Pago del servicio de agua", CONTRATO);
        recibo("401", "Telmex", "Internet y telefonía", "Pago de recibo Telmex", TELEFONO);
        recibo("402", "Izzi", "Internet y telefonía", "Pago de recibo Izzi", CONTRATO);
        recibo("403", "Totalplay", "Internet y telefonía", "Pago de recibo Totalplay", CONTRATO);
        recibo("404", "Megacable", "Internet y telefonía", "Pago de recibo Megacable", CONTRATO);
        recibo("501", "Sky", "Televisión", "Pago de servicio Sky", CONTRATO);
        recibo("502", "Dish", "Televisión", "Pago de servicio Dish", CONTRATO);
        recibo("601", "Naturgy", "Gas", "Pago de gas natural", CONTRATO);
        // Tarjetas de entretenimiento (precio fijo, referencia = correo o número de cliente)
        fijo("701", "Netflix", "Entretenimiento", CONTRATO, 150, 300);
        fijo("702", "Spotify", "Entretenimiento", CONTRATO, 115, 230);
        fijo("703", "Xbox", "Entretenimiento", CONTRATO, 200, 500);
        log.warn("Catálogo en modo SIMULADO ({} productos). Para usar Gestopago: GESTOPAGO_MODO=real", productos.size());
    }

    private void recarga(String idServicio, String marca, int... montos) {
        for (int monto : montos) {
            agregar(new ProductoCatalogo(idServicio, String.valueOf(monto), marca + " $" + monto, BigDecimal.valueOf(monto),
                    marca, "Recarga de tiempo aire " + marca + " por $" + monto, "Recargas", null, TELEFONO));
        }
    }

    private void recibo(String idServicio, String marca, String categoria, String descripcion, ReglaReferencia regla) {
        agregar(new ProductoCatalogo(idServicio, "1", marca, null, marca, descripcion, categoria, null, regla));
    }

    private void fijo(String idServicio, String marca, String categoria, ReglaReferencia regla, int... montos) {
        for (int monto : montos) {
            agregar(new ProductoCatalogo(idServicio, String.valueOf(monto), marca + " $" + monto, BigDecimal.valueOf(monto),
                    marca, "Tarjeta de regalo " + marca + " por $" + monto, categoria, null, regla));
        }
    }

    private void agregar(ProductoCatalogo p) {
        productos.put(p.idServicio() + "|" + p.idProducto(), p);
    }

    @Override
    public ProductoCatalogo validarProductoYMonto(String idServicio, String idProducto, BigDecimal monto) {
        ProductoCatalogo producto = obtener(idServicio, idProducto);
        if (monto == null || monto.signum() <= 0 || monto.stripTrailingZeros().scale() > 2) {
            throw new MontoNoPermitidoException("El monto debe ser mayor a cero y con máximo 2 decimales");
        }
        if (producto.tienePrecioFijo()) {
            if (monto.compareTo(producto.precio()) != 0) {
                throw new MontoNoPermitidoException("El monto no corresponde al precio del producto seleccionado");
            }
        } else if (monto.compareTo(properties.getMontoMinimo()) < 0 || monto.compareTo(properties.getMontoMaximo()) > 0) {
            throw new MontoNoPermitidoException("El monto debe estar entre " + properties.getMontoMinimo().toPlainString()
                    + " y " + properties.getMontoMaximo().toPlainString());
        }
        return producto;
    }

    @Override
    public ProductoCatalogo obtener(String idServicio, String idProducto) {
        return Optional.ofNullable(productos.get(idServicio + "|" + idProducto)).orElseThrow(ProductoNoDisponibleException::new);
    }

    @Override
    public List<ProductoCatalogo> listar() {
        return List.copyOf(productos.values());
    }
}
