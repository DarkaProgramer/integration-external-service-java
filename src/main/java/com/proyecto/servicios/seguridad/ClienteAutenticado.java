package com.proyecto.servicios.seguridad;

import java.util.Set;

/**
 * Identidad del cliente obtenida del token verificado. El clienteId nunca se toma del cuerpo de la petición.
 */
public record ClienteAutenticado(Long clienteId, Set<String> permisos) {

    public static final String PERMISO_PAGAR_SERVICIOS = "pagos:servicios";
    public static final String PERMISO_CONCILIAR = "pagos:conciliar";

    public ClienteAutenticado {
        permisos = permisos == null ? Set.of() : Set.copyOf(permisos);
    }

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }
}
