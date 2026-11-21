package com.proyecto.servicios.model.pagos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Resolución de un pago dudoso por un operador, después de confirmarlo con el proveedor
 * (portal, reporte de conciliación o soporte de Gestopago).
 */
@Data
public class ResolucionManualRequestDto {

    @NotBlank(message = "El folio es obligatorio")
    @Pattern(regexp = PatronesPago.UUID, message = "El folio debe ser un UUID")
    private String folio;

    @NotBlank(message = "El resultado es obligatorio")
    @Pattern(regexp = "^(APROBADA|RECHAZADA)$", message = "El resultado debe ser APROBADA o RECHAZADA")
    private String resultado;

    @Size(max = 64, message = "El folio del proveedor admite como máximo 64 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "El folio del proveedor solo admite letras, dígitos y guiones")
    private String folioProveedor;

    @Size(max = 64, message = "La autorización del proveedor admite como máximo 64 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "La autorización del proveedor solo admite letras, dígitos y guiones")
    private String autorizacionProveedor;
}
