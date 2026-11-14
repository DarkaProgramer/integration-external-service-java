package com.proyecto.servicios.model.auth;

import com.proyecto.servicios.entity.seguridad.TipoDispositivo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dispositivo desde el que se inicia sesión: se usa para avisar a los demás dispositivos de la cuenta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispositivoDto {

    @NotNull(message = "El tipo de dispositivo es obligatorio (WEB o MOVIL)")
    private TipoDispositivo tipo;

    @Size(max = 100, message = "El nombre del dispositivo no puede exceder 100 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N} .,_()/-]*$", message = "El nombre del dispositivo contiene caracteres no permitidos")
    private String nombre;
}
