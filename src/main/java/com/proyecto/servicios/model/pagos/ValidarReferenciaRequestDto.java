package com.proyecto.servicios.model.pagos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formato general; el formato específico del producto (longitud, solo dígitos) se valida contra el catálogo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidarReferenciaRequestDto {

    @NotBlank(message = "El ID del servicio es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El ID del servicio debe ser numérico")
    private String idServicio;

    @NotBlank(message = "El ID del producto es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El ID del producto debe ser numérico")
    private String idProducto;

    @NotBlank(message = "La referencia es obligatoria")
    @Size(max = 40, message = "La referencia no puede exceder 40 caracteres")
    private String referencia;
}
