package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resultado de validar la referencia: el validacionId se envía después en el pago. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidacionReferenciaResponseDto {
    private String validacionId;
    private String idServicio;
    private String idProducto;
    private String producto;
    private String servicio;
    /** Enmascarada: solo los últimos 4 caracteres. */
    private String referencia;
    /** Precio fijo del producto, si lo tiene. */
    private BigDecimal precio;
    /** Monto adeudado que informó el proveedor (recibos), si lo informó. */
    private BigDecimal montoAdeudo;
    private LocalDateTime expiraEn;
}
