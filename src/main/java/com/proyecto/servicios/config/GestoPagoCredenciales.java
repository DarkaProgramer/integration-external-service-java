package com.proyecto.servicios.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Valores por defecto del distribuidor de pruebas de Gestopago asignado al proyecto.
 * Se usan si GESTOPAGO_ID_DISTRIBUIDOR / GESTOPAGO_CODIGO_DISPOSITIVO faltan, están vacías o no son válidas,
 * para que la API obtenga el token aunque esas variables no se hayan cargado en Render.
 */
public final class GestoPagoCredenciales {

    public static final int ID_DISTRIBUIDOR_DEFECTO = 83;
    public static final String CODIGO_DISPOSITIVO_DEFECTO = "GPS83-TPV-17";

    private GestoPagoCredenciales() {
    }

    public static Integer idDistribuidor(String valor) {
        try {
            int id = Integer.parseInt(StringUtils.strip(valor));
            return id > 0 ? id : ID_DISTRIBUIDOR_DEFECTO;
        } catch (NumberFormatException | NullPointerException e) {
            return ID_DISTRIBUIDOR_DEFECTO;
        }
    }

    public static String codigoDispositivo(String valor) {
        return StringUtils.isBlank(valor) ? CODIGO_DISPOSITIVO_DEFECTO : valor.strip();
    }
}
