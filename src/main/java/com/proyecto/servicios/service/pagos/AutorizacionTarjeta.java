package com.proyecto.servicios.service.pagos;

/**
 * Cargo autorizado por el banco emisor. Solo contiene datos que se pueden guardar y mostrar.
 *
 * @param marca      VISA o MASTERCARD
 * @param ultimos4   últimos 4 dígitos de la tarjeta (lo único que se conserva del número)
 * @param codigo     código de autorización del banco
 */
public record AutorizacionTarjeta(String marca, String ultimos4, String codigo) {
}
