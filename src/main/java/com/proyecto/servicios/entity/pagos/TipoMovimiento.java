package com.proyecto.servicios.entity.pagos;

public enum TipoMovimiento {
    /** Descuento del monto del pago al reservar los fondos. */
    CARGO,
    /** Devolución del monto cuando el proveedor no procesó el pago. */
    REVERSO
}
