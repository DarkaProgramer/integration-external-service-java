package com.proyecto.servicios.model.onboarding;

/**
 * Expresiones regulares compartidas por los DTO para no repetir reglas de validación.
 */
public final class ValidacionPatrones {

    public static final String CURP = "^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$";
    public static final String RFC = "^[A-Z&Ñ]{3,4}\\d{6}[A-V0-9]{3}$";
    public static final String CURP_O_RFC = "^([A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}|[A-Z&Ñ]{3,4}\\d{6}[A-V0-9]{3})$";
    public static final String SOLO_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$";
    public static final String SOLO_LETRAS_OPCIONAL = "^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]*$";
    public static final String CORREO = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String TELEFONO = "^\\d{10}$";
    public static final String CODIGO_POSTAL = "^\\d{5}$";
    public static final String SEXO = "^[MF]$";
    public static final String ESTATUS_CUENTA = "^(ACTIVA|INACTIVA)$";

    private ValidacionPatrones() {
    }
}
