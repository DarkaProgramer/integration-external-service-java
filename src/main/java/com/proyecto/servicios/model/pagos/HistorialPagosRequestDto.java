package com.proyecto.servicios.model.pagos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Filtros del historial. Todos opcionales: sin cuerpo se devuelve la primera página con todos los pagos. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPagosRequestDto {

    @Min(value = 0, message = "La página empieza en 0")
    private Integer pagina = 0;

    @Min(value = 1, message = "El tamaño mínimo es 1")
    @Max(value = 50, message = "El tamaño máximo es 50")
    private Integer tamano = 20;

    /** EXITOSO, RECHAZADO o PENDIENTE. */
    private ResultadoPago resultado;
}
