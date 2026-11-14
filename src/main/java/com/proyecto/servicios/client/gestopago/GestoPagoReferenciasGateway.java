package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoReferenciasClient;
import com.proyecto.servicios.service.pagos.ProductoCatalogo;
import com.proyecto.servicios.service.pagos.ResultadoValidacionReferencia;
import com.proyecto.servicios.service.pagos.ValidadorReferenciaGateway;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Adaptador de Gestopago para validar referencias.
 * <ul>
 *     <li>Código de éxito configurado → VALIDA (con el monto adeudado si viene en la respuesta).</li>
 *     <li>Otro código → INVALIDA: el proveedor respondió, pero no reconoce la referencia.</li>
 *     <li>Sin ruta configurada, error HTTP, timeout, red o respuesta ilegible → NO_DISPONIBLE: falla cerrada,
 *     el pago no se habilita sin una validación real.</li>
 * </ul>
 * Los nombres de parámetros y campos son SUPUESTOS hasta tener el contrato oficial; están aislados aquí.
 */
@Slf4j
@Component
public class GestoPagoReferenciasGateway implements ValidadorReferenciaGateway {

    static final String PARAM_ID_SERVICIO = "idServicio";
    static final String PARAM_ID_PRODUCTO = "idProducto";
    static final String PARAM_REFERENCIA = "referencia";

    private final GestoPagoReferenciasClient client;
    private final String path;
    private final Set<String> codigosValidos;

    public GestoPagoReferenciasGateway(GestoPagoReferenciasClient client,
                                       @Value("${gestopago.referencia.path:}") String path,
                                       @Value("${gestopago.referencia.codigos-validos:0,00}") Set<String> codigosValidos) {
        this.client = client;
        this.path = path;
        this.codigosValidos = codigosValidos == null ? Set.of()
                : codigosValidos.stream().map(String::strip).filter(c -> !c.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ResultadoValidacionReferencia validar(ProductoCatalogo producto, String referencia) {
        if (StringUtils.isBlank(path) || "/".equals(path.strip())) {
            log.error("gestopago.referencia.path no está configurado: no se pueden validar referencias");
            return ResultadoValidacionReferencia.noDisponible("SIN_CONFIGURACION");
        }
        long inicio = System.nanoTime();
        ResultadoValidacionReferencia resultado;
        try {
            resultado = interpretar(client.consultar(parametros(producto, referencia)));
        } catch (FeignException e) {
            resultado = ResultadoValidacionReferencia.noDisponible("HTTP_" + e.status());
        } catch (RuntimeException e) {
            resultado = ResultadoValidacionReferencia.noDisponible("ERROR_" + e.getClass().getSimpleName());
        }
        // Nunca la referencia completa ni el cuerpo de la respuesta
        log.info("GestoPago validar referencia servicio={} producto={} estado={} codigo={} duracionMs={}",
                producto.idServicio(), producto.idProducto(), resultado.estado(), resultado.codigo(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
        return resultado;
    }

    ResultadoValidacionReferencia interpretar(String cuerpo) {
        RespuestaGestoPago respuesta;
        try {
            respuesta = RespuestaGestoPago.leer(cuerpo);
        } catch (RespuestaGestoPago.RespuestaIlegibleException e) {
            return ResultadoValidacionReferencia.noDisponible("RESPUESTA_ILEGIBLE");
        }
        Optional<String> codigo = respuesta.buscar("CODIGO", "codigoRespuesta", "codigo", "code");
        if (codigo.isEmpty()) {
            return ResultadoValidacionReferencia.noDisponible("SIN_CODIGO");
        }
        if (!codigosValidos.contains(codigo.get())) {
            return ResultadoValidacionReferencia.invalida(codigo.get());
        }
        BigDecimal monto = respuesta.buscar("MONTO", "montoPago", "importe", "saldo", "adeudo")
                .flatMap(GestoPagoReferenciasGateway::decimal)
                .orElse(null);
        return ResultadoValidacionReferencia.valida(monto, codigo.get());
    }

    private static Optional<BigDecimal> decimal(String valor) {
        try {
            BigDecimal monto = new BigDecimal(valor.strip());
            return monto.signum() > 0 ? Optional.of(monto) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> parametros(ProductoCatalogo producto, String referencia) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_SERVICIO, producto.idServicio());
        parametros.put(PARAM_ID_PRODUCTO, producto.idProducto());
        parametros.put(PARAM_REFERENCIA, referencia);
        return parametros;
    }
}
