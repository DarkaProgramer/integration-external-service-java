package com.proyecto.servicios.model.pagos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.entity.pagos.EstatusPago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante de la operación. La referencia va enmascarada; nunca incluye datos internos del proveedor. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoServicioResponseDto {

    private String folio;
    private EstatusPago estatus;
    private String idServicio;
    private String idProducto;
    private String producto;
    private String referencia;
    private BigDecimal monto;
    private String folioProveedor;
    private LocalDateTime fechaOperacion;
}
