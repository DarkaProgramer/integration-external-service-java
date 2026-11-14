package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Presentación del catálogo de Gestopago para los frontends.
 * <ul>
 *     <li>{@code logo-url}: plantilla para el logo cuando el catálogo no trae URL, p. ej.
 *     {@code https://cdn.ejemplo.com/logos/{idServicio}.png}. Vacía = sin logo (el front muestra las iniciales).</li>
 *     <li>{@code categorias}: nombre de cada id de tipo de servicio que envía Gestopago (p. ej. {@code 1=Recargas}).</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gestopago.catalogo")
public class GestoPagoCatalogoProperties {

    private String logoUrl = "";
    private Map<String, String> categorias = new HashMap<>();
}
