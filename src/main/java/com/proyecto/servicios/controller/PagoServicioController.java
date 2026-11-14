package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.model.comun.PaginaDto;
import com.proyecto.servicios.model.pagos.ConsultaPagoRequestDto;
import com.proyecto.servicios.model.pagos.HistorialPagosRequestDto;
import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.PagoServicioResponseDto;
import com.proyecto.servicios.model.pagos.ResolucionManualRequestDto;
import com.proyecto.servicios.model.pagos.ResultadoPago;
import com.proyecto.servicios.seguridad.ClienteActual;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.PagoServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pagos de servicios. Requiere {@code Authorization: Bearer <token del cliente>} y, para pagar,
 * {@code Idempotency-Key: <UUID>} generado por el cliente una vez por operación (y reutilizado en sus reintentos).
 * <ul>
 *     <li>201: pago aprobado.</li>
 *     <li>202: el proveedor no confirmó a tiempo; el monto queda retenido y el resultado se obtiene consultando el folio.</li>
 *     <li>422: rechazado por el proveedor; el monto se devolvió a la cuenta.</li>
 *     <li>200: repetición de una solicitud ya procesada (encabezado Idempotent-Replayed: true).</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/pagos/servicios", produces = MediaType.APPLICATION_JSON_VALUE)
public class PagoServicioController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final PagoServicioService pagoServicioService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> pagar(@ClienteActual ClienteAutenticado cliente,
                                                                      @RequestHeader(IDEMPOTENCY_KEY) String idempotencyKey,
                                                                      @Valid @RequestBody PagoServicioRequestDto solicitud) {
        PagoServicioService.ResultadoPagoServicio resultado = pagoServicioService.pagar(cliente, idempotencyKey, solicitud);
        PagoServicioResponseDto pago = resultado.pago();
        HttpStatus status = resultado.repetida() ? HttpStatus.OK : statusPara(pago.getEstatus());
        ApiResponse<PagoServicioResponseDto> cuerpo = ApiResponse.of(status.value(), mensajePara(pago.getEstatus()), pago);
        if (pago.getEstatus() == EstatusPago.RECHAZADA) {
            cuerpo.setCodigo(GenericResponse.CODIGO_ERROR);
        }
        return ResponseEntity.status(status)
                .header(IDEMPOTENT_REPLAYED, String.valueOf(resultado.repetida()))
                .body(cuerpo);
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> consultar(@ClienteActual ClienteAutenticado cliente,
                                                                          @Valid @RequestBody ConsultaPagoRequestDto consulta) {
        PagoServicioResponseDto pago = pagoServicioService.consultar(cliente, consulta.getFolio());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), mensajePara(pago.getEstatus()), pago));
    }

    /** Historial del usuario autenticado (más reciente primero). El cuerpo es opcional: {pagina, tamano, resultado}. */
    @PostMapping(value = "/historial")
    public ResponseEntity<ApiResponse<PaginaDto<PagoServicioResponseDto>>> historial(
            @ClienteActual ClienteAutenticado cliente,
            @Valid @RequestBody(required = false) HistorialPagosRequestDto filtros) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Historial de pagos",
                pagoServicioService.historial(cliente, filtros)));
    }

    /** Uso interno de operaciones: requiere el permiso pagos:conciliar en el token. */
    @PostMapping(value = "/conciliacion/resolver", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PagoServicioResponseDto>> resolver(@ClienteActual ClienteAutenticado operador,
                                                                         @Valid @RequestBody ResolucionManualRequestDto resolucion) {
        PagoServicioResponseDto pago = pagoServicioService.resolverManualmente(operador, resolucion);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), mensajePara(pago.getEstatus()), pago));
    }

    static HttpStatus statusPara(EstatusPago estatus) {
        return switch (estatus) {
            case APROBADA -> HttpStatus.CREATED;
            case RECHAZADA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDIENTE, EN_CONCILIACION, REVISION_MANUAL -> HttpStatus.ACCEPTED;
        };
    }

    static String mensajePara(EstatusPago estatus) {
        return ResultadoPago.mensaje(estatus);
    }
}
