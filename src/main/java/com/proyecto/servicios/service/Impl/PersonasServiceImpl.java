package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.exception.PersonaNoEncontradaException;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonasServiceImpl implements PersonaService {

    private final PersonasRepository personasRepository;

    @Override
    @Transactional
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoP(personasRequest.getApellidoP());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        return toResponse(personasRepository.save(persona));
    }

    @Override
    @Transactional
    public PersonaResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        Personas persona = buscarPorNombre(eliminaPersonaRequest.getNombre());
        personasRepository.delete(persona);
        return toResponse(persona);
    }

    @Override
    @Transactional
    public PersonaResponse actualizaPersona(PersonasRequest personasRequest) {
        Personas persona = buscarPorNombre(personasRequest.getNombre());
        persona.setApellidoP(personasRequest.getApellidoP());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        return toResponse(personasRepository.save(persona));
    }

    private Personas buscarPorNombre(String nombre) {
        return personasRepository.findByNombre(nombre)
                .orElseThrow(() -> new PersonaNoEncontradaException("No existe una persona con el nombre: " + nombre));
    }

    private PersonaResponse toResponse(Personas persona) {
        return new PersonaResponse(persona.getNombre(), persona.getApellidoP(), persona.getApellidoMaterno());
    }
}
