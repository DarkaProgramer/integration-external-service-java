package com.proyecto.servicios.client.gestopago;

import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.service.pagos.ProveedorPagosGateway;
import com.proyecto.servicios.service.pagos.ResultadoProveedor;
import com.proyecto.servicios.service.pagos.SolicitudPagoProveedor;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador de Gestopago para el puerto {@link ProveedorPagosGateway}.
 * <p>
 * Regla central: <b>solo se considera RECHAZADA una operación cuando hay certeza de que el proveedor no la
 * procesó</b>; cualquier otra falla queda INDETERMINADA y se concilia, porque devolver el dinero de un pago que
 * el proveedor sí aplicó es tan grave como cobrar dos veces.
 *
 * <table>
 *     <tr><th>Situación</th><th>Resultado</th><th>Por qué</th></tr>
 *     <tr><td>2xx con código de éxito configurado</td><td>APROBADA</td><td>confirmación explícita</td></tr>
 *     <tr><td>2xx con código de rechazo configurado</td><td>RECHAZADA</td><td>rechazo explícito del proveedor</td></tr>
 *     <tr><td>2xx con código desconocido o cuerpo ilegible</td><td>INDETERMINADA</td><td>no se puede interpretar</td></tr>
 *     <tr><td>400, 401, 403, 404, 405, 415, 422, 429</td><td>RECHAZADA</td><td>la petición se rechazó antes de procesarse</td></tr>
 *     <tr><td>408, 409, 5xx y otros</td><td>INDETERMINADA</td><td>pudo procesarse (p. ej. 504 de un balanceador)</td></tr>
 *     <tr><td>conexión rechazada, host desconocido, sin token</td><td>RECHAZADA</td><td>la petición nunca salió</td></tr>
 *     <tr><td>timeout de lectura, conexión cortada, otro error</td><td>INDETERMINADA</td><td>pudo llegar y procesarse</td></tr>
 * </table>
 */
@Slf4j
@Component
public class GestoPagoPagosGateway implements ProveedorPagosGateway {

    /** SUPUESTO sobre los nombres de parámetros de sendTx.do (no hay contrato público). Ajustar aquí. */
    static final String PARAM_ID_SERVICIO = "idServicio";
    static final String PARAM_ID_PRODUCTO = "idProducto";
    static final String PARAM_REFERENCIA = "referencia";
    static final String PARAM_MONTO = "montoPago";
    static final String PARAM_FOLIO_CLIENTE = "idTransaccionCliente";

    private static final Set<Integer> HTTP_NO_PROCESADO = Set.of(400, 401, 403, 404, 405, 415, 422, 429);

    private final GestoPagoTransaccionesClient client;
    private final Set<String> codigosAprobada;
    private final Set<String> codigosRechazo;

    public GestoPagoPagosGateway(GestoPagoTransaccionesClient client,
                                 @Value("${gestopago.tx.codigos-aprobada:0,00}") Set<String> codigosAprobada,
                                 @Value("${gestopago.tx.codigos-rechazo:}") Set<String> codigosRechazo) {
        this.client = client;
        this.codigosAprobada = limpiar(codigosAprobada);
        this.codigosRechazo = limpiar(codigosRechazo);
    }

    @Override
    public ResultadoProveedor enviarPago(SolicitudPagoProveedor solicitud) {
        long inicio = System.nanoTime();
        ResultadoProveedor resultado;
        try {
            resultado = interpretar(client.sendTx(parametros(solicitud)));
        } catch (FeignException e) {
            resultado = clasificar(e);
        } catch (GestoPagoIntegrationException e) {
            // Lanzada por el interceptor antes de enviar (token no configurado): la petición no salió
            resultado = ResultadoProveedor.rechazada("CONFIG_" + e.getErrorType().name());
        } catch (RuntimeException e) {
            resultado = ResultadoProveedor.indeterminada("ERROR_" + e.getClass().getSimpleName());
        }
        // Solo folio interno, estado, código y duración: nunca referencia, token ni cuerpo de la respuesta
        log.info("GestoPago sendTx folio={} estado={} codigo={} duracionMs={}", solicitud.folio(), resultado.estado(),
                resultado.codigo(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
        return resultado;
    }

    /**
     * Gestopago no publica un endpoint de consulta de transacciones (al menos no en la información disponible).
     * Por eso no se adivina: los pagos dudosos agotan sus intentos y pasan a revisión manual, donde un operador
     * los resuelve con el reporte o el portal del proveedor. Cuando exista el endpoint, se implementa aquí
     * y la conciliación automática empieza a funcionar sin cambiar el resto del código.
     */
    @Override
    public Optional<ResultadoProveedor> consultarEstado(SolicitudPagoProveedor solicitud) {
        return Optional.empty();
    }

    ResultadoProveedor interpretar(String cuerpo) {
        RespuestaGestoPago respuesta;
        try {
            respuesta = RespuestaGestoPago.leer(cuerpo);
        } catch (RespuestaGestoPago.RespuestaIlegibleException e) {
            return ResultadoProveedor.indeterminada("RESPUESTA_ILEGIBLE");
        }
        Optional<String> codigo = respuesta.buscar("CODIGO", "codigoRespuesta", "codigo", "code", "status");
        if (codigo.isEmpty()) {
            return ResultadoProveedor.indeterminada("SIN_CODIGO");
        }
        String valor = codigo.get();
        if (codigosAprobada.contains(valor)) {
            String folio = respuesta.buscar("TRANSACCION", "idTransaccion", "folio", "NUM_AUTORIZACION", "autorizacion")
                    .orElse(null);
            return ResultadoProveedor.aprobada(folio, valor);
        }
        if (codigosRechazo.contains(valor)) {
            return ResultadoProveedor.rechazada(valor);
        }
        return ResultadoProveedor.indeterminada(valor);
    }

    ResultadoProveedor clasificar(FeignException e) {
        if (e instanceof RetryableException) {
            return nuncaSalio(e)
                    ? ResultadoProveedor.rechazada("SIN_CONEXION")
                    : ResultadoProveedor.indeterminada("TIMEOUT_O_CORTE");
        }
        int status = e.status();
        if (HTTP_NO_PROCESADO.contains(status)) {
            if (status == 401 || status == 403) {
                log.error("GestoPago rechazó las credenciales (HTTP {}): revisar el token configurado", status);
            }
            return ResultadoProveedor.rechazada("HTTP_" + status);
        }
        return ResultadoProveedor.indeterminada("HTTP_" + status);
    }

    private boolean nuncaSalio(Throwable error) {
        for (Throwable actual = error; actual != null; actual = actual.getCause()) {
            if (actual instanceof ConnectException || actual instanceof UnknownHostException
                    || actual instanceof NoRouteToHostException) {
                return true;
            }
            if (actual.getCause() == actual) {
                break;
            }
        }
        return false;
    }

    private Map<String, String> parametros(SolicitudPagoProveedor solicitud) {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put(PARAM_ID_SERVICIO, solicitud.idServicio());
        parametros.put(PARAM_ID_PRODUCTO, solicitud.idProducto());
        parametros.put(PARAM_REFERENCIA, solicitud.referencia());
        parametros.put(PARAM_MONTO, solicitud.monto().setScale(2, RoundingMode.UNNECESSARY).toPlainString());
        parametros.put(PARAM_FOLIO_CLIENTE, solicitud.folio());
        return parametros;
    }

    private static Set<String> limpiar(Set<String> codigos) {
        return codigos == null ? Set.of() : codigos.stream().map(String::strip).filter(c -> !c.isEmpty())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
