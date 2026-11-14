package com.proyecto.servicios.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequestDto {

    @NotBlank(message = "El refreshToken es obligatorio")
    @Size(max = 200, message = "refreshToken inválido")
    private String refreshToken;
}
