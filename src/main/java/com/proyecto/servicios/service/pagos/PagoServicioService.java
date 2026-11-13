package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.PagoServicioResponseDto;
import com.proyecto.servicios.model.pagos.ResolucionManualRequestDto;
import com.proyecto.servicios.seguridad.ClienteAutenticado;

public interface PagoServicioService {

    /**
     * Ejecuta un pago de servicio de forma idempotente: la misma llave con los mismos datos devuelve el
     * resultado original sin volver a cobrar ni a enviar la operación al proveedor.
     */
    ResultadoPagoServicio pagar(ClienteAutenticado cliente, String idempotencyKey, PagoServicioRequestDto solicitud);

    PagoServicioResponseDto consultar(ClienteAutenticado cliente, String folio);

    /** Resolución de un pago dudoso por un operador con permiso de conciliación. */
    PagoServicioResponseDto resolverManualmente(ClienteAutenticado operador, ResolucionManualRequestDto resolucion);

    /**
     * @param repetida true si la respuesta corresponde a una solicitud ya procesada con la misma llave
     */
    record ResultadoPagoServicio(PagoServicioResponseDto pago, boolean repetida) {
    }
}
