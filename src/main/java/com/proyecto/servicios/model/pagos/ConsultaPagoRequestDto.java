package com.proyecto.servicios.model.pagos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConsultaPagoRequestDto {

    @NotBlank(message = "El folio es obligatorio")
    @Pattern(regexp = PatronesPago.UUID, message = "El folio debe ser un UUID")
    private String folio;
}
