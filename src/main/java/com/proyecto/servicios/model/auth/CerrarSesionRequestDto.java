package com.proyecto.servicios.model.auth;

import com.proyecto.servicios.model.pagos.PatronesPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CerrarSesionRequestDto {

    @NotBlank(message = "El sesionId es obligatorio")
    @Pattern(regexp = PatronesPago.UUID, message = "El sesionId debe ser un UUID")
    private String sesionId;
}
