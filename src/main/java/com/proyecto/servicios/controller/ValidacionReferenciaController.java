package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.model.pagos.ValidacionReferenciaResponseDto;
import com.proyecto.servicios.model.pagos.ValidarReferenciaRequestDto;
import com.proyecto.servicios.seguridad.ClienteActual;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.ValidacionReferenciaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paso previo obligatorio al pago: valida el formato de la referencia y la consulta a Gestopago.
 * Devuelve un validacionId (vigencia corta) que el pago debe incluir.
 * <ul>
 *     <li>200: referencia válida.</li>
 *     <li>422: formato inválido o el proveedor no la reconoce.</li>
 *     <li>503: no se pudo consultar al proveedor (no se habilita el pago).</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class ValidacionReferenciaController {

    private final ValidacionReferenciaService validacionReferenciaService;

    @PostMapping(value = "/api/pagos/referencias/validar", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ValidacionReferenciaResponseDto>> validar(@ClienteActual ClienteAutenticado cliente,
                                                                                @Valid @RequestBody ValidarReferenciaRequestDto solicitud) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Referencia válida",
                validacionReferenciaService.validar(cliente, solicitud)));
    }
}
