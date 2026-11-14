package com.proyecto.servicios.service.pagos;

import java.math.BigDecimal;

/**
 * Respuesta clasificada de la consulta de referencia al proveedor.
 *
 * @param montoAdeudo monto a pagar si el proveedor lo informa (p. ej. recibo de luz); puede ser {@code null}
 * @param codigo      código del proveedor o de transporte, solo para uso interno
 */
public record ResultadoValidacionReferencia(Estado estado, BigDecimal montoAdeudo, String codigo) {

    public enum Estado {
        /** El proveedor reconoce la referencia. */
        VALIDA,
        /** El proveedor respondió que la referencia no existe o no se puede pagar. */
        INVALIDA,
        /** No se pudo consultar (sin configuración, timeout, error de red o respuesta ilegible). */
        NO_DISPONIBLE
    }

    public static ResultadoValidacionReferencia valida(BigDecimal montoAdeudo, String codigo) {
        return new ResultadoValidacionReferencia(Estado.VALIDA, montoAdeudo, codigo);
    }

    public static ResultadoValidacionReferencia invalida(String codigo) {
        return new ResultadoValidacionReferencia(Estado.INVALIDA, null, codigo);
    }

    public static ResultadoValidacionReferencia noDisponible(String codigo) {
        return new ResultadoValidacionReferencia(Estado.NO_DISPONIBLE, null, codigo);
    }
}
