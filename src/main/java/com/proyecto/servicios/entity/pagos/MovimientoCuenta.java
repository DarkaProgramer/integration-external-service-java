package com.proyecto.servicios.entity.pagos;

import com.proyecto.servicios.entity.client.Cuenta;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitácora de movimientos de saldo. Cada cambio a {@code cuentas.saldo} originado por un pago deja un registro,
 * lo que permite auditar y reconstruir el saldo.
 */
@Entity
@Table(name = "movimientos_cuenta",
        uniqueConstraints = @UniqueConstraint(name = "uq_movimiento_pago_tipo", columnNames = {"pago_id", "tipo"}))
@Getter
@Setter
@NoArgsConstructor
public class MovimientoCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    /** Un pago tiene a lo más un CARGO y un REVERSO (restricción única): impide devolver dos veces. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", nullable = false)
    private PagoServicio pago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MovimientoCuenta(Cuenta cuenta, PagoServicio pago, TipoMovimiento tipo, BigDecimal monto) {
        this.cuenta = cuenta;
        this.pago = pago;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoResultante = cuenta.getSaldo();
    }

    @PrePersist
    void alCrear() {
        createdAt = LocalDateTime.now();
    }
}
