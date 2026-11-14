package com.proyecto.servicios.seguridad;

import java.util.Set;

/**
 * Identidad del cliente obtenida del token verificado. El clienteId nunca se toma del cuerpo de la petición.
 *
 * @param sesionId sesión (dispositivo) a la que pertenece el token; {@code null} solo en tokens de desarrollo
 */
public record ClienteAutenticado(Long clienteId, Set<String> permisos, String sesionId) {

    public static final String PERMISO_PAGAR_SERVICIOS = "pagos:servicios";
    public static final String PERMISO_CONCILIAR = "pagos:conciliar";

    public ClienteAutenticado {
        permisos = permisos == null ? Set.of() : Set.copyOf(permisos);
    }

    public ClienteAutenticado(Long clienteId, Set<String> permisos) {
        this(clienteId, permisos, null);
    }

    public boolean tienePermiso(String permiso) {
        return permisos.contains(permiso);
    }
}
