package com.proyecto.servicios.service.pagos;

/**
 * Resultado clasificado de una operación con el proveedor.
 *
 * @param codigo código del proveedor o de transporte (p. ej. "HTTP_503", "TIMEOUT"); solo para uso interno
 */
public record ResultadoProveedor(Estado estado, String folioProveedor, String codigo) {

    public enum Estado {
        /** El proveedor confirmó que procesó el pago. */
        APROBADA,
        /** Hay certeza de que el proveedor NO procesó el pago (rechazo explícito o la petición nunca salió). */
        RECHAZADA,
        /** No se puede saber si lo procesó: timeout, 5xx, conexión cortada, respuesta ilegible o código desconocido. */
        INDETERMINADA
    }

    public static ResultadoProveedor aprobada(String folioProveedor, String codigo) {
        return new ResultadoProveedor(Estado.APROBADA, folioProveedor, codigo);
    }

    public static ResultadoProveedor rechazada(String codigo) {
        return new ResultadoProveedor(Estado.RECHAZADA, null, codigo);
    }

    public static ResultadoProveedor indeterminada(String codigo) {
        return new ResultadoProveedor(Estado.INDETERMINADA, null, codigo);
    }
}
