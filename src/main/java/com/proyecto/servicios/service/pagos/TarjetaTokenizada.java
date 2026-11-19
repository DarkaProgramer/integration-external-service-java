package com.proyecto.servicios.service.pagos;

/**
 * Tarjeta ya validada por el procesador. El {@code token} sustituye al número completo, que nunca se guarda.
 */
public record TarjetaTokenizada(String token, String marca, String ultimos4, int mesVencimiento, int anioVencimiento,
                                String titular) {
}
