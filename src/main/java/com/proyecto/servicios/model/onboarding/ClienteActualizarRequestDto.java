package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ClienteActualizarRequestDto {
    @NotBlank(message = "El CURP o RFC es obligatorio para identificar al cliente a actualizar")
    private String identificadorFiscalBusqueda;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String correo;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
    private String telefono;

    public String getIdentificadorFiscalBusqueda() { return identificadorFiscalBusqueda; }
    public void setIdentificadorFiscalBusqueda(String identificadorFiscalBusqueda) { this.identificadorFiscalBusqueda = identificadorFiscalBusqueda; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
}