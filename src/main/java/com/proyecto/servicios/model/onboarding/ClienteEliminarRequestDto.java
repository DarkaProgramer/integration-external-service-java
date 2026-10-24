package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ClienteEliminarRequestDto {

    @NotBlank(message = "El CURP o RFC es obligatorio para realizar la baja")
    @Pattern(regexp = ValidacionPatrones.CURP_O_RFC, message = "Debe proporcionar una CURP (18 caracteres) o un RFC (12 o 13 caracteres) válido")
    private String identificadorFiscal;
}
