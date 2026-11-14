package com.proyecto.servicios.model.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "password")
public class LoginRequestDto {

    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no puede exceder 72 caracteres")
    private String password;

    @NotNull(message = "Los datos del dispositivo son obligatorios")
    @Valid
    private DispositivoDto dispositivo;
}
