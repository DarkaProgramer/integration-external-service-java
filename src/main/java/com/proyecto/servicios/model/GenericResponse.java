package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta base del API (estilo visto en clase): todas las respuestas, exitosas o de error,
 * incluyen un código y un mensaje.
 * <ul>
 *     <li>{@link #CODIGO_EXITO} (0): la operación se realizó correctamente.</li>
 *     <li>{@link #CODIGO_ERROR} (1): la operación no se pudo realizar; el mensaje explica el motivo.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse {

    public static final int CODIGO_EXITO = 0;
    public static final int CODIGO_ERROR = 1;

    private Integer codigo;
    private String mensaje;
}
