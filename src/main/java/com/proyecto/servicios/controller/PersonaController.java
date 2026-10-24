package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.model.comun.ApiResponse;
import com.proyecto.servicios.service.PersonaService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public class PersonaController {

    private final PersonaService personaService;

    @PostMapping
    public ResponseEntity<ApiResponse<PersonaResponse>> crear(@Valid @RequestBody PersonasRequest personasRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value(), "Persona creada correctamente",
                        personaService.creaPersona(personasRequest)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PersonaResponse>> actualizar(@Valid @RequestBody PersonasRequest personasRequest) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Persona actualizada correctamente",
                personaService.actualizaPersona(personasRequest)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<PersonaResponse>> eliminar(@Valid @RequestBody EliminaPersonaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), "Persona eliminada correctamente",
                personaService.eliminaPersona(request)));
    }
}
