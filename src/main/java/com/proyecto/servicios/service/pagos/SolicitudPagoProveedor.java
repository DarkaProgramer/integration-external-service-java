package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.entity.pagos.PagoServicio;

import java.math.BigDecimal;

/** Datos mínimos que se envían al proveedor. El folio interno viaja para que el proveedor pueda detectar reenvíos. */
public record SolicitudPagoProveedor(String folio, String idServicio, String idProducto, String referencia, BigDecimal monto) {

    public static SolicitudPagoProveedor de(PagoServicio pago) {
        return new SolicitudPagoProveedor(pago.getFolio(), pago.getIdServicio(), pago.getIdProducto(),
                pago.getReferencia(), pago.getMonto());
    }
}
