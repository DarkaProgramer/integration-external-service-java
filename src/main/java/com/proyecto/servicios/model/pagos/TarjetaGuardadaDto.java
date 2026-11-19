package com.proyecto.servicios.model.pagos;

import lombok.Builder;
import lombok.Getter;

/** Tarjeta guardada tal como se muestra al cliente (sin número completo ni CVV). */
@Getter
@Builder
public class TarjetaGuardadaDto {

    /** Identificador opaco de la tarjeta (token del procesador). Se envía como tarjetaGuardadaId al pagar. */
    private String id;
    private String marca;
    private String ultimos4;
    /** MM/AA */
    private String vencimiento;
    private String titular;
    private boolean vencida;
}
