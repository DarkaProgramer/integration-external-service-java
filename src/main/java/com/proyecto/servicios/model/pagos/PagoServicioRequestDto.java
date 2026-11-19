package com.proyecto.servicios.model.pagos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Datos que envía el cliente para pagar un servicio. Solo contiene lo que el cliente puede decidir:
 * el estatus, el folio, la comisión, etc. los asigna el servidor (y los campos desconocidos se rechazan).
 * Estas reglas validan el formato; que el producto exista y el monto corresponda se valida contra el catálogo.
 */
@Data
public class PagoServicioRequestDto {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^\\d{10,20}$", message = "El número de cuenta debe tener entre 10 y 20 dígitos")
    private String numeroCuenta;

    @NotBlank(message = "El ID del servicio es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El ID del servicio debe ser numérico")
    private String idServicio;

    @NotBlank(message = "El ID del producto es obligatorio")
    @Pattern(regexp = "^\\d{1,10}$", message = "El ID del producto debe ser numérico")
    private String idProducto;

    @NotBlank(message = "La referencia es obligatoria")
    @Pattern(regexp = "^[A-Za-z0-9]{4,40}$", message = "La referencia debe tener entre 4 y 40 letras o dígitos, sin espacios ni símbolos")
    private String referencia;

    /** Comprobante devuelto por POST /api/pagos/referencias/validar: sin él no se paga. */
    @NotBlank(message = "Valida la referencia antes de pagar: el validacionId es obligatorio")
    @Pattern(regexp = PatronesPago.UUID, message = "El validacionId debe ser un UUID")
    private String validacionId;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @DecimalMax(value = "9999999.99", message = "El monto excede el máximo permitido")
    @Digits(integer = 7, fraction = 2, message = "El monto admite como máximo 2 decimales")
    private BigDecimal monto;

    /**
     * Tarjeta de débito con la que se cobra el pago (de contado, en una sola exhibición). Si no se envía,
     * se usa el método anterior: cargo al saldo de la cuenta (lo usa la app móvil).
     */
    @Valid
    @ToString.Exclude
    private TarjetaDto tarjeta;
}
