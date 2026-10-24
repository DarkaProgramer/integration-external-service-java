package com.proyecto.servicios.service;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;

public interface PersonaService {
    PersonaResponse creaPersona(PersonasRequest personasRequest);
    PersonaResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest);
    PersonaResponse actualizaPersona(PersonasRequest personasRequest);
}
