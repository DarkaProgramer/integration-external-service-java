package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;

public class ClienteConsultaRequestDto {
    @NotBlank(message = "El CURP o RFC es obligatorio para la consulta")
    private String identificadorFiscal;

    public String getIdentificadorFiscal() { return identificadorFiscal; }
    public void setIdentificadorFiscal(String identificadorFiscal) { this.identificadorFiscal = identificadorFiscal; }
}