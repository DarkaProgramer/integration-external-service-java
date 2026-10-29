package com.proyecto.servicios.client.gestopago;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Árbol simple para leer las respuestas de Gestopago sin atarse a su formato exacto.
 * <p>
 * El catálogo de Gestopago responde en XML ({@code RESPONSE/MENSAJE/CODIGO,TEXTO} y {@code PRODUCTOS/producto});
 * para sendTx.do no hay documentación pública, así que se acepta XML o JSON y los campos se buscan por nombre
 * sin distinguir mayúsculas. El XML se procesa con DTD y entidades externas deshabilitadas (protección XXE).
 */
public record RespuestaGestoPago(String nombre, Map<String, String> valores, List<RespuestaGestoPago> hijos) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RespuestaGestoPago leer(String cuerpo) {
        if (cuerpo == null || cuerpo.isBlank()) {
            throw new RespuestaIlegibleException();
        }
        String texto = cuerpo.strip();
        try {
            if (texto.startsWith("<")) {
                return desdeXml(texto);
            }
            if (texto.startsWith("{") || texto.startsWith("[")) {
                return desdeJson("", MAPPER.readTree(texto));
            }
        } catch (RespuestaIlegibleException e) {
            throw e;
        } catch (Exception e) {
            throw new RespuestaIlegibleException();
        }
        throw new RespuestaIlegibleException();
    }

    /** Primer valor encontrado (recorrido en profundidad) para cualquiera de las claves. */
    public Optional<String> buscar(String... claves) {
        for (String clave : claves) {
            Optional<String> valor = buscarClave(clave.toLowerCase(Locale.ROOT));
            if (valor.isPresent()) {
                return valor;
            }
        }
        return Optional.empty();
    }

    public List<RespuestaGestoPago> buscarNodos(Predicate<RespuestaGestoPago> criterio) {
        List<RespuestaGestoPago> encontrados = new ArrayList<>();
        recolectar(criterio, encontrados);
        return encontrados;
    }

    /** Valor propio del nodo (atributo o hijo simple), sin descender. */
    public Optional<String> valor(String... claves) {
        for (String clave : claves) {
            String v = valores.get(clave.toLowerCase(Locale.ROOT));
            if (v != null && !v.isBlank()) {
                return Optional.of(v.strip());
            }
        }
        return Optional.empty();
    }

    private Optional<String> buscarClave(String clave) {
        String v = valores.get(clave);
        if (v != null && !v.isBlank()) {
            return Optional.of(v.strip());
        }
        for (RespuestaGestoPago hijo : hijos) {
            Optional<String> encontrado = hijo.buscarClave(clave);
            if (encontrado.isPresent()) {
                return encontrado;
            }
        }
        return Optional.empty();
    }

    private void recolectar(Predicate<RespuestaGestoPago> criterio, List<RespuestaGestoPago> acumulado) {
        if (criterio.test(this)) {
            acumulado.add(this);
        }
        hijos.forEach(h -> h.recolectar(criterio, acumulado));
    }

    // ───────────── XML ─────────────

    private static RespuestaGestoPago desdeXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document documento = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        return desdeElemento(documento.getDocumentElement());
    }

    private static RespuestaGestoPago desdeElemento(Element elemento) {
        Map<String, String> valores = new LinkedHashMap<>();
        List<RespuestaGestoPago> hijos = new ArrayList<>();
        NamedNodeMap atributos = elemento.getAttributes();
        for (int i = 0; i < atributos.getLength(); i++) {
            Node atributo = atributos.item(i);
            valores.put(atributo.getNodeName().toLowerCase(Locale.ROOT), atributo.getNodeValue());
        }
        NodeList nodos = elemento.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i) instanceof Element hijo) {
                if (tieneElementosHijos(hijo) || hijo.hasAttributes()) {
                    hijos.add(desdeElemento(hijo));
                } else {
                    valores.putIfAbsent(hijo.getTagName().toLowerCase(Locale.ROOT), hijo.getTextContent());
                }
            }
        }
        return new RespuestaGestoPago(elemento.getTagName(), valores, hijos);
    }

    private static boolean tieneElementosHijos(Element elemento) {
        NodeList nodos = elemento.getChildNodes();
        for (int i = 0; i < nodos.getLength(); i++) {
            if (nodos.item(i) instanceof Element) {
                return true;
            }
        }
        return false;
    }

    // ───────────── JSON ─────────────

    private static RespuestaGestoPago desdeJson(String nombre, JsonNode nodo) {
        Map<String, String> valores = new LinkedHashMap<>();
        List<RespuestaGestoPago> hijos = new ArrayList<>();
        if (nodo.isArray()) {
            nodo.forEach(elemento -> hijos.add(desdeJson(nombre, elemento)));
        } else if (nodo.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> campos = nodo.fields();
            while (campos.hasNext()) {
                Map.Entry<String, JsonNode> campo = campos.next();
                JsonNode valor = campo.getValue();
                if (valor.isValueNode()) {
                    valores.put(campo.getKey().toLowerCase(Locale.ROOT), valor.isNull() ? null : valor.asText());
                } else {
                    hijos.add(desdeJson(campo.getKey(), valor));
                }
            }
        }
        return new RespuestaGestoPago(nombre, valores, hijos);
    }

    /** La respuesta no es XML/JSON válido. No incluye el contenido para no filtrarlo a logs. */
    public static class RespuestaIlegibleException extends RuntimeException {
        public RespuestaIlegibleException() {
            super("Respuesta del proveedor ilegible");
        }
    }
}
