package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CorreoYaRegistradoException;
import com.proyecto.servicios.exception.ReglaNegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.onboarding.ClienteActualizarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteResponseDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.ClienteSpecifications;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    static final int EDAD_MINIMA = 18;
    static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");
    static final String CUENTA_ACTIVA = "ACTIVA";
    static final String CUENTA_INACTIVA = "INACTIVA";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public ClienteResponseDto registrar(ClienteRegistroRequestDto dto) {
        validarMayorDeEdad(dto.getFechaNacimiento());

        if (clienteRepository.existsByCurp(dto.getCurp())) {
            throw new ClienteYaRegistradoException("Ya existe un cliente registrado con la CURP: " + dto.getCurp());
        }
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            throw new ClienteYaRegistradoException("Ya existe un cliente registrado con el RFC: " + dto.getRfc());
        }
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new CorreoYaRegistradoException("El correo " + dto.getCorreo() + " ya está registrado por otro cliente");
        }

        Cliente cliente = clienteMapper.toEntity(dto);
        // Campos controlados por el servidor
        cliente.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(SALDO_INICIAL);
        cuenta.setEstatus(CUENTA_ACTIVA);
        cuenta.setCliente(cliente);
        cliente.setCuenta(cuenta);

        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> buscar(ClienteConsultaRequestDto filtros) {
        return clienteRepository.findAll(ClienteSpecifications.conFiltros(filtros), Sort.by("id"))
                .stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClienteResponseDto actualizar(ClienteActualizarRequestDto dto) {
        Cliente cliente = obtenerPorIdentificador(dto.getIdentificadorFiscal());

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("No es posible actualizar un cliente que fue dado de baja");
        }
        if (dto.getCorreo() != null && clienteRepository.existsByCorreoAndIdNot(dto.getCorreo(), cliente.getId())) {
            throw new CorreoYaRegistradoException("El correo " + dto.getCorreo() + " ya está registrado por otro cliente");
        }

        clienteMapper.actualizarEntidad(dto, cliente);
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public ClienteResponseDto darDeBaja(String identificadorFiscal) {
        Cliente cliente = obtenerPorIdentificador(identificadorFiscal);

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ReglaNegocioException("El cliente con identificador " + identificadorFiscal + " ya se encuentra dado de baja");
        }

        cliente.setActivo(false);
        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus(CUENTA_INACTIVA);
        }
        return clienteMapper.toResponse(clienteRepository.save(cliente));
    }

    private Cliente obtenerPorIdentificador(String identificadorFiscal) {
        return clienteRepository.findFirstByCurpOrRfc(identificadorFiscal, identificadorFiscal)
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "No existe un cliente con la CURP o RFC: " + identificadorFiscal));
    }

    private void validarMayorDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ReglaNegocioException("El cliente debe ser mayor de edad (" + EDAD_MINIMA + " años o más)");
        }
    }

    private String generarNumeroCuentaUnico() {
        String numero;
        do {
            numero = String.format("%010d", (long) (RANDOM.nextDouble() * 10_000_000_000L));
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }
}
