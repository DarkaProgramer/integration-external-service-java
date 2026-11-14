package com.proyecto.servicios.model.auth;

import com.proyecto.servicios.entity.seguridad.TipoDispositivo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AlertaSesionDto {
    private Long id;
    private String mensaje;
    /** Sesión nueva: se puede cerrar con /api/auth/sesiones/cerrar si el usuario no la reconoce. */
    private String sesionId;
    private TipoDispositivo tipoDispositivo;
    private String nombreDispositivo;
    private LocalDateTime fecha;
}
