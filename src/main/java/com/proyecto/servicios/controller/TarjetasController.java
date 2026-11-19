package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.model.pagos.TarjetaDto;
import com.proyecto.servicios.model.pagos.TarjetaGuardadaDto;
import com.proyecto.servicios.seguridad.ClienteActual;
import com.proyecto.servicios.seguridad.ClienteAutenticado;
import com.proyecto.servicios.service.pagos.TarjetasService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tarjetas de débito guardadas del cliente (solo token, marca, terminación y vencimiento). */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/tarjetas", produces = MediaType.APPLICATION_JSON_VALUE)
public class TarjetasController {

    private final TarjetasService tarjetasService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TarjetaGuardadaDto>>> listar(@ClienteActual ClienteAutenticado cliente) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Tarjetas guardadas", tarjetasService.listar(cliente)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TarjetaGuardadaDto>> guardar(@ClienteActual ClienteAutenticado cliente,
                                                                   @Valid @RequestBody TarjetaDto tarjeta) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "Tarjeta guardada", tarjetasService.guardar(cliente, tarjeta)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@ClienteActual ClienteAutenticado cliente, @PathVariable String id) {
        tarjetasService.eliminar(cliente, id);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Tarjeta eliminada", null));
    }
}
