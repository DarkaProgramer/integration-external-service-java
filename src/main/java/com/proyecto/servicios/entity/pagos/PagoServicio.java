package com.proyecto.servicios.entity.pagos;

import com.proyecto.servicios.entity.client.Cuenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de cada pago de servicio. Es la fuente de verdad para la idempotencia (llave única por cliente),
 * la conciliación con el proveedor y la auditoría.
 * La referencia se guarda completa porque se necesita para conciliar, pero nunca se escribe en logs ni se
 * devuelve sin enmascarar.
 */
@Entity
@Table(name = "pagos_servicio",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pagos_cliente_idempotency", columnNames = {"cliente_id", "idempotency_key"}),
                @UniqueConstraint(name = "uq_pagos_folio", columnNames = "folio")
        },
        indexes = {
                @Index(name = "idx_pagos_estatus", columnList = "estatus, updated_at"),
                @Index(name = "idx_pagos_cuenta_fecha", columnList = "cuenta_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class PagoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador público de la operación (UUID). Es el que se comparte con el cliente y con el proveedor. */
    @Column(nullable = false, length = 36)
    private String folio;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /** SHA-256 de los datos de la solicitud: detecta que una misma llave se reutilice con otros datos. */
    @Column(name = "huella_solicitud", nullable = false, length = 64)
    private String huellaSolicitud;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "id_servicio", nullable = false, length = 10)
    private String idServicio;

    @Column(name = "id_producto", nullable = false, length = 10)
    private String idProducto;

    @Column(name = "nombre_producto", length = 150)
    private String nombreProducto;

    @Column(name = "nombre_servicio", length = 150)
    private String nombreServicio;

    @Column(nullable = false, length = 40)
    private String referencia;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstatusPago estatus;

    @Column(name = "folio_proveedor", length = 64)
    private String folioProveedor;

    /** Código de respuesta del proveedor o de la capa HTTP (p. ej. HTTP_504). Solo para soporte interno. */
    @Column(name = "codigo_proveedor", length = 30)
    private String codigoProveedor;

    /** TARJETA_DEBITO o SALDO (método anterior). De la tarjeta solo se guardan marca y últimos 4 dígitos. */
    @Column(name = "metodo_pago", length = 20)
    private String metodoPago;

    @Column(name = "tarjeta_marca", length = 20)
    private String tarjetaMarca;

    @Column(name = "tarjeta_ultimos4", length = 4)
    private String tarjetaUltimos4;

    @Column(name = "autorizacion_tarjeta", length = 12)
    private String autorizacionTarjeta;

    @Column(name = "intentos_conciliacion", nullable = false)
    private int intentosConciliacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Bloqueo optimista adicional: evita que dos procesos apliquen resultados distintos sobre el mismo pago. */
    @Version
    private Long version;

    @PrePersist
    void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = ahora;
        }
        updatedAt = ahora;
    }

    @PreUpdate
    void alActualizar() {
        updatedAt = LocalDateTime.now();
    }
}
