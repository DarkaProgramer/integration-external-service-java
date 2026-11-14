package com.proyecto.servicios.model.pagos;

import com.proyecto.servicios.entity.pagos.EstatusPago;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lo que la interfaz debe mostrar. EXITOSO solo cuando Gestopago confirmó (APROBADA); mientras no haya
 * confirmación el pago es PENDIENTE, nunca exitoso.
 */
public enum ResultadoPago {
    EXITOSO(EnumSet.of(EstatusPago.APROBADA)),
    RECHAZADO(EnumSet.of(EstatusPago.RECHAZADA)),
    PENDIENTE(EnumSet.of(EstatusPago.PENDIENTE, EstatusPago.EN_CONCILIACION, EstatusPago.REVISION_MANUAL));

    private final Set<EstatusPago> estatus;

    ResultadoPago(Set<EstatusPago> estatus) {
        this.estatus = estatus;
    }

    public Set<EstatusPago> estatus() {
        return estatus;
    }

    public static ResultadoPago de(EstatusPago estatus) {
        return switch (estatus) {
            case APROBADA -> EXITOSO;
            case RECHAZADA -> RECHAZADO;
            case PENDIENTE, EN_CONCILIACION, REVISION_MANUAL -> PENDIENTE;
        };
    }

    public static String mensaje(EstatusPago estatus) {
        return switch (estatus) {
            case APROBADA -> "Pago realizado correctamente";
            case RECHAZADA -> "El proveedor rechazó el pago; el monto fue devuelto a su cuenta";
            case PENDIENTE, EN_CONCILIACION -> "El pago está en proceso de confirmación con el proveedor; el monto permanece retenido. Consulte el folio más tarde";
            case REVISION_MANUAL -> "El pago está en revisión; el monto permanece retenido hasta confirmar el resultado con el proveedor";
        };
    }
}
