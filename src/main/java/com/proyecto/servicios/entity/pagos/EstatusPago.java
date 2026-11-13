package com.proyecto.servicios.entity.pagos;

/**
 * Ciclo de vida de un pago de servicio.
 * <pre>
 * PENDIENTE ──► APROBADA
 *     │
 *     ├──────► RECHAZADA          (el proveedor no lo procesó: se devuelve el dinero)
 *     │
 *     └──────► EN_CONCILIACION ──► APROBADA | RECHAZADA | REVISION_MANUAL
 * </pre>
 * Solo APROBADA y RECHAZADA son finales. En PENDIENTE, EN_CONCILIACION y REVISION_MANUAL
 * el monto permanece retenido: nunca se devuelve sin confirmar que el proveedor no lo procesó.
 */
public enum EstatusPago {

    /** Fondos descontados; la operación se está enviando al proveedor. */
    PENDIENTE,
    /** El proveedor confirmó la operación. */
    APROBADA,
    /** El proveedor no procesó la operación; el monto ya se devolvió a la cuenta. */
    RECHAZADA,
    /** No se sabe si el proveedor la procesó (timeout, 5xx, respuesta ilegible). */
    EN_CONCILIACION,
    /** Se agotaron los intentos automáticos de conciliación; requiere intervención de un operador. */
    REVISION_MANUAL;

    public boolean esFinal() {
        return this == APROBADA || this == RECHAZADA;
    }
}
