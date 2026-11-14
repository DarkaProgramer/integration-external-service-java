package com.proyecto.servicios.entity.pagos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Comprobante de que Gestopago validó una referencia para un cliente y un producto. El pago exige un comprobante
 * vigente, del mismo cliente, producto y referencia, y lo consume (un comprobante = un pago).
 */
@Entity
@Table(name = "validaciones_referencia", indexes = @Index(name = "idx_validaciones_cliente", columnList = "cliente_id, created_at"))
@Getter
@Setter
@NoArgsConstructor
public class ValidacionReferencia {

    /** UUID que el frontend envía al pagar (validacionId). */
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "id_servicio", nullable = false, length = 10)
    private String idServicio;

    @Column(name = "id_producto", nullable = false, length = 10)
    private String idProducto;

    @Column(nullable = false, length = 40)
    private String referencia;

    /** Monto a pagar informado por el proveedor (recibos), si lo devuelve. */
    @Column(name = "monto_adeudo", precision = 15, scale = 2)
    private BigDecimal montoAdeudo;

    @Column(name = "codigo_proveedor", length = 30)
    private String codigoProveedor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "usada_en")
    private LocalDateTime usadaEn;

    @Column(name = "folio_pago", length = 36)
    private String folioPago;
}
