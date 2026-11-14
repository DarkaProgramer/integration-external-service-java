package com.proyecto.servicios.model.auth;

import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Alta de usuario: los datos del cliente (mismas reglas que el onboarding) más la contraseña y el dispositivo.
 * Al registrarse se abre la primera sesión, igual que en un login.
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = "password")
public class RegistroRequestDto extends ClienteRegistroRequestDto {

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PasswordReglas.PATRON, message = PasswordReglas.MENSAJE)
    private String password;

    @NotNull(message = "Los datos del dispositivo son obligatorios")
    @Valid
    private DispositivoDto dispositivo;
}
