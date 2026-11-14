package com.proyecto.servicios.entity.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Aviso para una sesión ya abierta de que la cuenta inició sesión en otro dispositivo.
 * Los dispositivos la consultan periódicamente (polling) y la marcan como leída.
 */
@Entity
@Table(name = "alertas_sesion", indexes = @Index(name = "idx_alertas_destino", columnList = "sesion_destino_id, leida"))
@Getter
@Setter
@NoArgsConstructor
public class AlertaSesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    /** Sesión que debe recibir el aviso. */
    @Column(name = "sesion_destino_id", nullable = false, length = 36)
    private String sesionDestinoId;

    /** Sesión nueva que provocó el aviso. */
    @Column(name = "sesion_origen_id", nullable = false, length = 36)
    private String sesionOrigenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dispositivo", nullable = false, length = 10)
    private TipoDispositivo tipoDispositivo;

    @Column(name = "nombre_dispositivo", length = 100)
    private String nombreDispositivo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private boolean leida;
}
