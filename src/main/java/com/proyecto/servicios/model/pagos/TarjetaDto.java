package com.proyecto.servicios.model.pagos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/**
 * Tarjeta de débito con la que se paga. Solo viaja en la petición: nunca se guarda ni se registra en logs
 * el número completo ni el CVV (de la tarjeta solo se conservan la marca y los últimos 4 dígitos).
 */
@Data
public class TarjetaDto {

    @ToString.Exclude
    @NotBlank(message = "El número de tarjeta es obligatorio")
    @Pattern(regexp = "^[\\d ]{13,23}$", message = "El número de tarjeta solo admite dígitos")
    private String numero;

    @NotBlank(message = "El nombre del titular es obligatorio")
    @Size(min = 5, max = 60, message = "El nombre del titular debe tener entre 5 y 60 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ .'-]+$", message = "El nombre del titular solo admite letras")
    private String titular;

    @NotNull(message = "El mes de vencimiento es obligatorio")
    @Min(value = 1, message = "Mes de vencimiento inválido")
    @Max(value = 12, message = "Mes de vencimiento inválido")
    private Integer mesVencimiento;

    @NotNull(message = "El año de vencimiento es obligatorio")
    @Min(value = 2000, message = "Año de vencimiento inválido")
    @Max(value = 2100, message = "Año de vencimiento inválido")
    private Integer anioVencimiento;

    @ToString.Exclude
    @NotBlank(message = "El CVV es obligatorio")
    @Pattern(regexp = "^\\d{3,4}$", message = "El CVV debe tener 3 o 4 dígitos")
    private String cvv;
}
