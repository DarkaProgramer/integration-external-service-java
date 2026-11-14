package com.proyecto.servicios.entity.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una sesión por dispositivo. El access token (JWT corto) lleva el id de la sesión, así que cerrar la sesión
 * invalida el token de inmediato. El refresh token solo se guarda como SHA-256 y se rota en cada uso.
 */
@Entity
@Table(name = "sesiones_cliente",
        uniqueConstraints = @UniqueConstraint(name = "uq_sesiones_refresh", columnNames = "refresh_hash"),
        indexes = @Index(name = "idx_sesiones_cliente", columnList = "cliente_id, cerrada_en"))
@Getter
@Setter
@NoArgsConstructor
public class SesionCliente {

    /** UUID público de la sesión (claim "sid" del token). */
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dispositivo", nullable = false, length = 10)
    private TipoDispositivo tipoDispositivo;

    @Column(name = "nombre_dispositivo", length = 100)
    private String nombreDispositivo;

    @Column(name = "refresh_hash", nullable = false, length = 64)
    private String refreshHash;

    @Column(name = "refresh_expira", nullable = false)
    private LocalDateTime refreshExpira;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ultimo_uso", nullable = false)
    private LocalDateTime ultimoUso;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @Column(name = "motivo_cierre", length = 30)
    private String motivoCierre;

    public boolean activaEn(LocalDateTime ahora) {
        return cerradaEn == null && refreshExpira.isAfter(ahora);
    }
}
