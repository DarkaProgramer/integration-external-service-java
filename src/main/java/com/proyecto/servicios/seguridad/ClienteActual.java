package com.proyecto.servicios.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Inyecta en un parámetro del controller el {@link ClienteAutenticado} del token Bearer de la petición.
 * Si el token falta o es inválido, la petición termina con 401 antes de ejecutar el método.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClienteActual {
}
