package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.model.pagos.TarjetaDto;

import java.math.BigDecimal;

/** Puerto hacia el procesador de pagos con tarjeta (adquirente). */
public interface ProcesadorTarjetas {

    /**
     * Valida los datos de una tarjeta de débito y devuelve un token que la representa (el número completo y el
     * CVV no se conservan).
     *
     * @throws com.proyecto.servicios.exception.pagos.TarjetaInvalidaException si los datos no son válidos o no es de débito
     */
    TarjetaTokenizada tokenizar(TarjetaDto tarjeta);

    /**
     * Autoriza un cargo de contado (una sola exhibición) a una tarjeta tokenizada. El CVV se pide en cada pago.
     *
     * @throws com.proyecto.servicios.exception.pagos.TarjetaInvalidaException  si la tarjeta venció o el CVV no es válido
     * @throws com.proyecto.servicios.exception.pagos.TarjetaRechazadaException si el banco declina el cargo
     */
    AutorizacionTarjeta autorizar(TarjetaTokenizada tarjeta, String cvv, BigDecimal monto);

    /** Devuelve a la tarjeta un cargo autorizado (p. ej. cuando el proveedor del servicio rechaza el pago). */
    void reembolsar(AutorizacionTarjeta autorizacion, BigDecimal monto);
}
