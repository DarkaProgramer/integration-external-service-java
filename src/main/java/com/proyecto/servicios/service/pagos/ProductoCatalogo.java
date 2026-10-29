package com.proyecto.servicios.service.pagos;

import java.math.BigDecimal;

/**
 * Producto del catálogo del proveedor, en el formato interno (independiente del XML/JSON de Gestopago).
 *
 * @param precio precio fijo del producto; {@code null} o 0 significa que el monto es variable (p. ej. pago de un recibo)
 */
public record ProductoCatalogo(String idServicio, String idProducto, String nombre, BigDecimal precio) {

    public boolean tienePrecioFijo() {
        return precio != null && precio.signum() > 0;
    }
}
