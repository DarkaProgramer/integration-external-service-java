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
    /** Agrupación simple para la interfaz: EXITOSO, RECHAZADO o PENDIENTE. */
    private ResultadoPago resultado;
    /** Texto para mostrar al usuario según el estatus. */
    private String mensaje;
    private String idServicio;
    private String idProducto;
    private String producto;
    private String servicio;
    private String referencia;
    private BigDecimal monto;
    private String folioProveedor;
    private LocalDateTime fechaOperacion;
}
