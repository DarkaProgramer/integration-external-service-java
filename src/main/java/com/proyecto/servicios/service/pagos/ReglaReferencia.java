package com.proyecto.servicios.service.pagos;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Formato de la referencia que acepta un producto (longitud y si solo admite dígitos), tomado del catálogo del
 * proveedor cuando lo informa. Se valida antes de consultar a Gestopago para no enviar referencias imposibles.
 */
public record ReglaReferencia(int longitudMinima, int longitudMaxima, boolean soloDigitos) {

    public static final int MINIMO_GENERAL = 4;
    public static final int MAXIMO_GENERAL = 40;
    public static final ReglaReferencia GENERAL = new ReglaReferencia(MINIMO_GENERAL, MAXIMO_GENERAL, false);

    private static final Pattern DIGITOS = Pattern.compile("^\\d+$");
    private static final Pattern ALFANUMERICO = Pattern.compile("^[A-Za-z0-9]+$");

    public ReglaReferencia {
        longitudMinima = Math.max(1, longitudMinima);
        longitudMaxima = Math.max(longitudMinima, Math.min(longitudMaxima, MAXIMO_GENERAL));
    }

    /** @return el mensaje de error si la referencia no cumple el formato; vacío si es válida */
    public Optional<String> errores(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return Optional.of("La referencia es obligatoria");
        }
        if (soloDigitos && !DIGITOS.matcher(referencia).matches()) {
            return Optional.of("La referencia de este servicio solo admite dígitos");
        }
        if (!ALFANUMERICO.matcher(referencia).matches()) {
            return Optional.of("La referencia solo admite letras y dígitos, sin espacios ni símbolos");
        }
        int largo = referencia.length();
        if (largo < longitudMinima || largo > longitudMaxima) {
            return Optional.of(longitudMinima == longitudMaxima
                    ? "La referencia debe tener exactamente " + longitudMinima + " caracteres"
                    : "La referencia debe tener entre " + longitudMinima + " y " + longitudMaxima + " caracteres");
        }
        return Optional.empty();
    }
}
