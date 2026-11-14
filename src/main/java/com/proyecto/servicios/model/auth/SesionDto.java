package com.proyecto.servicios.model.auth;

import com.proyecto.servicios.entity.seguridad.TipoDispositivo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SesionDto {
    private String sesionId;
    private TipoDispositivo tipoDispositivo;
    private String nombreDispositivo;
    private LocalDateTime inicio;
    private LocalDateTime ultimoUso;
    /** true si es la sesión que hace la consulta. */
    private boolean actual;
}
