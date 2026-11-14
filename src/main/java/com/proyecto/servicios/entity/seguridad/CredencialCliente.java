package com.proyecto.servicios.entity.seguridad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Credenciales de acceso del cliente (las mismas para web y app). La contraseña solo se guarda como hash BCrypt.
 * Tras varios intentos fallidos el acceso se bloquea temporalmente (protección contra fuerza bruta).
 */
@Entity
@Table(name = "credenciales_cliente")
@Getter
@Setter
@NoArgsConstructor
public class CredencialCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false, unique = true)
    private Long clienteId;

    /** Correo en minúsculas: es el usuario del login. */
    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void alCrear() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
