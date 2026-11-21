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
    /** Número de autorización que devolvió Gestopago al aplicar el pago. */
    private String autorizacionProveedor;
    /** TARJETA_DEBITO o SALDO. */
    private String metodoPago;
    /** Tarjeta enmascarada, p. ej. "VISA •••• 1234". */
    private String tarjeta;
    /** Código de autorización del banco (cargo a la tarjeta). */
    private String autorizacion;
    private LocalDateTime fechaOperacion;
}
