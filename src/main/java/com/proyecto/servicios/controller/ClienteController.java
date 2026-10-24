package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.model.onboarding.ClienteActualizarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteEliminarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteResponseDto;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de administración de clientes (usuarios).
 * Toda la información se recibe en el cuerpo mediante DTOs validados con @Valid;
 * los errores los procesa GlobalExceptionHandler, por eso aquí no hay try-catch.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> registrar(@Valid @RequestBody ClienteRegistroRequestDto request) {
        ClienteResponseDto cliente = clienteService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "Cliente registrado correctamente", cliente));
    }

    /**
     * Único endpoint de consulta: todos los filtros son opcionales y se pueden combinar.
     * Se usa POST porque los criterios viajan en el cuerpo de la petición.
     */
    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<ClienteResponseDto>>> buscar(@Valid @RequestBody ClienteConsultaRequestDto filtros) {
        List<ClienteResponseDto> clientes = clienteService.buscar(filtros);
        String mensaje = clientes.isEmpty()
                ? "No se encontraron clientes con los criterios indicados"
                : "Se encontraron " + clientes.size() + " cliente(s)";
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), mensaje, clientes));
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> actualizar(@Valid @RequestBody ClienteActualizarRequestDto request) {
        ClienteResponseDto cliente = clienteService.actualizar(request);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Cliente actualizado correctamente", cliente));
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ClienteResponseDto>> darDeBaja(@Valid @RequestBody ClienteEliminarRequestDto request) {
        ClienteResponseDto cliente = clienteService.darDeBaja(request.getIdentificadorFiscal());
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Cliente dado de baja correctamente", cliente));
    }
}
