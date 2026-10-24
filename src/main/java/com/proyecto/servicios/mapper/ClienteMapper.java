package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.model.onboarding.ClienteActualizarRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.model.onboarding.ClienteResponseDto;
import com.proyecto.servicios.model.onboarding.CuentaResponseDto;
import com.proyecto.servicios.model.onboarding.DomicilioDto;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Conversión entre DTOs y entidades del módulo de clientes.
 * Nunca copia campos controlados por el servidor (id, activo, fechas, cuenta).
 */
@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteRegistroRequestDto dto) {
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellidoPaterno(dto.getApellidoPaterno());
        cliente.setApellidoMaterno(dto.getApellidoMaterno());
        cliente.setFechaNacimiento(dto.getFechaNacimiento());
        cliente.setCurp(dto.getCurp());
        cliente.setRfc(dto.getRfc());
        cliente.setSexo(dto.getSexo());
        cliente.setNacionalidad(dto.getNacionalidad());
        cliente.setEstadoCivil(dto.getEstadoCivil());
        cliente.setCorreo(dto.getCorreo());
        cliente.setTelefonoMovil(dto.getTelefonoMovil());
        cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());
        cliente.setOcupacion(dto.getOcupacion());
        cliente.setEmpresa(dto.getEmpresa());
        cliente.setIngresoMensual(dto.getIngresoMensual());

        Domicilio domicilio = new Domicilio();
        copiarDomicilio(dto.getDomicilio(), domicilio);
        domicilio.setCliente(cliente);
        cliente.setDomicilio(domicilio);
        return cliente;
    }

    /** Aplica sobre la entidad sólo los campos que vienen informados en la petición. */
    public void actualizarEntidad(ClienteActualizarRequestDto dto, Cliente cliente) {
        asignarSiPresente(dto.getNombre(), cliente::setNombre);
        asignarSiPresente(dto.getSegundoNombre(), cliente::setSegundoNombre);
        asignarSiPresente(dto.getApellidoPaterno(), cliente::setApellidoPaterno);
        asignarSiPresente(dto.getApellidoMaterno(), cliente::setApellidoMaterno);
        asignarSiPresente(dto.getSexo(), cliente::setSexo);
        asignarSiPresente(dto.getNacionalidad(), cliente::setNacionalidad);
        asignarSiPresente(dto.getEstadoCivil(), cliente::setEstadoCivil);
        asignarSiPresente(dto.getCorreo(), cliente::setCorreo);
        asignarSiPresente(dto.getTelefonoMovil(), cliente::setTelefonoMovil);
        asignarSiPresente(dto.getTelefonoAlternativo(), cliente::setTelefonoAlternativo);
        asignarSiPresente(dto.getOcupacion(), cliente::setOcupacion);
        asignarSiPresente(dto.getEmpresa(), cliente::setEmpresa);
        asignarSiPresente(dto.getIngresoMensual(), cliente::setIngresoMensual);

        if (dto.getDomicilio() != null) {
            Domicilio domicilio = cliente.getDomicilio();
            if (domicilio == null) {
                domicilio = new Domicilio();
                domicilio.setCliente(cliente);
                cliente.setDomicilio(domicilio);
            }
            copiarDomicilio(dto.getDomicilio(), domicilio);
        }
    }

    public ClienteResponseDto toResponse(Cliente cliente) {
        return ClienteResponseDto.builder()
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                .correo(cliente.getCorreo())
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                .activo(cliente.getActivo())
                .fechaRegistro(cliente.getCreatedAt())
                .fechaActualizacion(cliente.getUpdatedAt())
                .domicilio(toDomicilioDto(cliente.getDomicilio()))
                .cuenta(toCuentaResponse(cliente.getCuenta()))
                .build();
    }

    private DomicilioDto toDomicilioDto(Domicilio domicilio) {
        if (domicilio == null) {
            return null;
        }
        DomicilioDto dto = new DomicilioDto();
        dto.setCalle(domicilio.getCalle());
        dto.setNumeroExterior(domicilio.getNumeroExterior());
        dto.setNumeroInterior(domicilio.getNumeroInterior());
        dto.setColonia(domicilio.getColonia());
        dto.setMunicipio(domicilio.getMunicipio());
        dto.setEstado(domicilio.getEstado());
        dto.setCodigoPostal(domicilio.getCodigoPostal());
        dto.setPais(domicilio.getPais());
        return dto;
    }

    private CuentaResponseDto toCuentaResponse(Cuenta cuenta) {
        if (cuenta == null) {
            return null;
        }
        return CuentaResponseDto.builder()
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .estatus(cuenta.getEstatus())
                .fechaApertura(cuenta.getCreatedAt())
                .build();
    }

    private void copiarDomicilio(DomicilioDto origen, Domicilio destino) {
        destino.setCalle(origen.getCalle());
        destino.setNumeroExterior(origen.getNumeroExterior());
        destino.setNumeroInterior(origen.getNumeroInterior());
        destino.setColonia(origen.getColonia());
        destino.setMunicipio(origen.getMunicipio());
        destino.setEstado(origen.getEstado());
        destino.setCodigoPostal(origen.getCodigoPostal());
        destino.setPais(origen.getPais());
    }

    private static <T> void asignarSiPresente(T valor, Consumer<T> setter) {
        if (valor != null) {
            setter.accept(valor);
        }
    }
}
