package com.proyecto.servicios.entity.pagos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tarjeta de débito guardada por el cliente. Solo contiene el token del procesador y datos para mostrarla:
 * nunca el número completo ni el CVV (el CVV se pide en cada pago).
 */
@Entity
@Table(name = "tarjetas_guardadas",
        uniqueConstraints = @UniqueConstraint(name = "uq_tarjetas_token", columnNames = "token"),
        indexes = @Index(name = "idx_tarjetas_cliente", columnList = "cliente_id, activa"))
@Getter
@Setter
@NoArgsConstructor
public class TarjetaGuardada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false, length = 20)
    private String marca;

    @Column(nullable = false, length = 4)
    private String ultimos4;

    @Column(name = "mes_vencimiento", nullable = false)
    private Integer mesVencimiento;

    @Column(name = "anio_vencimiento", nullable = false)
    private Integer anioVencimiento;

    @Column(length = 60)
    private String titular;

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
