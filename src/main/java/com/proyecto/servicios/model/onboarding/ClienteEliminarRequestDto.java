package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;

public class ClienteEliminarRequestDto {
    @NotBlank(message = "El CURP o RFC es obligatorio para realizar la baja")
    private String identificadorFiscal;

    public String getIdentificadorFiscal() { return identificadorFiscal; }
    public void setIdentificadorFiscal(String identificadorFiscal) { this.identificadorFiscal = identificadorFiscal; }
}