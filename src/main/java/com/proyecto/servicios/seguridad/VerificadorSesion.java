package com.proyecto.servicios.seguridad;

/** Comprueba que la sesión del token siga abierta; lanza 401 si fue cerrada o expiró. */
public interface VerificadorSesion {

    void verificarActiva(ClienteAutenticado cliente);
}
