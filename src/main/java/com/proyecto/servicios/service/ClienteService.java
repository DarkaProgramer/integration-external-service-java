package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteActualizarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteResponseDto;

import java.util.List;

public interface ClienteService {

    ClienteResponseDto registrar(ClienteRegistroRequestDto request);

    /** Búsqueda dinámica: combina todos los filtros informados en la petición. */
    List<ClienteResponseDto> buscar(ClienteConsultaRequestDto filtros);

    ClienteResponseDto actualizar(ClienteActualizarRequestDto request);

    /** Baja lógica del cliente identificado por CURP o RFC. */
    ClienteResponseDto darDeBaja(String identificadorFiscal);
}
