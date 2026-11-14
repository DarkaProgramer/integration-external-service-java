package com.proyecto.servicios.model.auth;

/** Política de contraseñas: 8 a 72 caracteres (límite de BCrypt), con al menos una letra y un dígito. */
public final class PasswordReglas {

    public static final String PATRON = "^(?=.*\\p{L})(?=.*\\d).{8,72}$";
    public static final String MENSAJE = "La contraseña debe tener entre 8 y 72 caracteres, con al menos una letra y un número";

    private PasswordReglas() {
    }
}
