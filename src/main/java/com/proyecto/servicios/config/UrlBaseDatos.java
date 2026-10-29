package com.proyecto.servicios.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Convierte la URL de conexión que entregan los proveedores de nube al formato JDBC.
 * <ul>
 *     <li>Render / Heroku: {@code postgres://usuario:clave@host:5432/db} o {@code postgresql://...}</li>
 *     <li>Railway: {@code mysql://usuario:clave@host:3306/db}</li>
 *     <li>Una URL que ya empieza con {@code jdbc:} se usa tal cual.</li>
 * </ul>
 * Si la URL trae usuario y contraseña, se separan para pasarlos al pool (los JDBC no siempre los aceptan en la URL).
 */
public record UrlBaseDatos(String jdbcUrl, String usuario, String clave) {

    public static UrlBaseDatos desde(String url, String usuarioPorDefecto, String clavePorDefecto) {
        if (url == null || url.isBlank() || url.startsWith("jdbc:")) {
            return new UrlBaseDatos(url, usuarioPorDefecto, clavePorDefecto);
        }
        URI uri = URI.create(url.strip());
        String esquema = switch (uri.getScheme()) {
            case "postgres", "postgresql" -> "postgresql";
            case "mysql" -> "mysql";
            default -> throw new IllegalArgumentException("Esquema de base de datos no soportado: " + uri.getScheme());
        };
        StringBuilder jdbc = new StringBuilder("jdbc:").append(esquema).append("://").append(uri.getHost());
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append(uri.getRawPath() == null ? "" : uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }
        String usuario = usuarioPorDefecto;
        String clave = clavePorDefecto;
        if (uri.getRawUserInfo() != null) {
            String[] partes = uri.getRawUserInfo().split(":", 2);
            usuario = decodificar(partes[0]);
            clave = partes.length > 1 ? decodificar(partes[1]) : clavePorDefecto;
        }
        return new UrlBaseDatos(jdbc.toString(), usuario, clave);
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }

    /** Para logs: nunca incluye usuario ni contraseña. */
    @Override
    public String toString() {
        return jdbcUrl;
    }
}
