package com.proyecto.servicios.service.pagos;

import java.math.BigDecimal;

/**
 * Producto del catálogo del proveedor, en el formato interno (independiente del XML/JSON de Gestopago).
 *
 * @param precio      precio fijo del producto; {@code null} o 0 significa que el monto es variable (p. ej. pago de un recibo)
 * @param servicio    nombre del proveedor del servicio (Telcel, CFE...)
 * @param categoria   tipo de servicio (recargas, luz, agua...)
 * @param logoUrl     logo del proveedor; {@code null} si Gestopago no lo informa y no hay plantilla configurada
 * @param referencia  formato de la referencia que acepta el producto
 */
public record ProductoCatalogo(String idServicio, String idProducto, String nombre, BigDecimal precio,
                               String servicio, String descripcion, String categoria, String logoUrl,
                               ReglaReferencia referencia) {

    public ProductoCatalogo {
        referencia = referencia == null ? ReglaReferencia.GENERAL : referencia;
    }

    public ProductoCatalogo(String idServicio, String idProducto, String nombre, BigDecimal precio) {
        this(idServicio, idProducto, nombre, precio, null, null, null, null, ReglaReferencia.GENERAL);
    }

    public boolean tienePrecioFijo() {
        return precio != null && precio.signum() > 0;
    }

    /** Identificador único del producto para los frontends: "idServicio-idProducto". */
    public String identificador() {
        return idServicio + "-" + idProducto;
    }
}
