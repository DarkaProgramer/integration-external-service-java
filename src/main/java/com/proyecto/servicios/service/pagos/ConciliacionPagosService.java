package com.proyecto.servicios.service.pagos;

public interface ConciliacionPagosService {

    /**
     * Revisa los pagos cuyo resultado se desconoce, consulta al proveedor cuando es posible y aplica el resultado.
     * Nunca reenvía la operación: un reintento a ciegas podría cobrar dos veces.
     *
     * @return número de pagos que quedaron con resultado final en esta ejecución
     */
    int conciliarPendientes();
}
