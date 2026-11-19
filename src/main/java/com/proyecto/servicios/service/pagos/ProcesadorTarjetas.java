package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.model.pagos.TarjetaDto;

import java.math.BigDecimal;

/** Puerto hacia el procesador de pagos con tarjeta (adquirente). */
public interface ProcesadorTarjetas {

    /**
     * Autoriza un cargo de contado (una sola exhibición) a una tarjeta de débito.
     *
     * @throws com.proyecto.servicios.exception.pagos.TarjetaInvalidaException  si los datos de la tarjeta no son válidos
     *                                                                         o no es de débito
     * @throws com.proyecto.servicios.exception.pagos.TarjetaRechazadaException si el banco declina el cargo
     */
    AutorizacionTarjeta autorizar(TarjetaDto tarjeta, BigDecimal monto);

    /** Devuelve a la tarjeta un cargo autorizado (p. ej. cuando el proveedor del servicio rechaza el pago). */
    void reembolsar(AutorizacionTarjeta autorizacion, BigDecimal monto);
}
