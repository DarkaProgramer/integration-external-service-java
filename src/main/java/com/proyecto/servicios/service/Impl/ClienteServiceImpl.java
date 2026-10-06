package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Domicilio;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.repositorys.ClienteRepository;
import com.proyecto.servicios.repositorys.CuentaRepository;
import com.proyecto.servicios.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class ClienteServiceImpl implements ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Override
    @Transactional
    public Cliente registrarCliente(ClienteRegistroRequestDto dto) {
        int edad = Period.between(dto.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            throw new IllegalArgumentException("El cliente debe ser mayor de edad (18 años o más)");
        }

        if (clienteRepository.existsByCurp(dto.getCurp())) {
            throw new ClienteYaRegistradoException("Ya existe un cliente registrado con la CURP: " + dto.getCurp());
        }
        if (clienteRepository.existsByRfc(dto.getRfc())) {
            throw new ClienteYaRegistradoException("Ya existe un cliente registrado con el RFC: " + dto.getRfc());
        }
        if (clienteRepository.existsByCorreo(dto.getCorreo())) {
            throw new ClienteYaRegistradoException("Ya existe un cliente registrado con el correo: " + dto.getCorreo());
        }

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
        cliente.setActivo(true);

        Domicilio domicilio = new Domicilio();
        domicilio.setCalle(dto.getDomicilio().getCalle());
        domicilio.setNumeroExterior(dto.getDomicilio().getNumeroExterior());
        domicilio.setNumeroInterior(dto.getDomicilio().getNumeroInterior());
        domicilio.setColonia(dto.getDomicilio().getColonia());
        domicilio.setMunicipio(dto.getDomicilio().getMunicipio());
        domicilio.setEstado(dto.getDomicilio().getEstado());
        domicilio.setCodigoPostal(dto.getDomicilio().getCodigoPostal());
        domicilio.setPais(dto.getDomicilio().getPais());
        domicilio.setCliente(cliente);
        cliente.setDomicilio(domicilio);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(BigDecimal.valueOf(1000.00));
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(cliente);
        cliente.setCuenta(cuenta);

        return clienteRepository.save(cliente);
    }

    @Override
    public List<Cliente> consultarTodosClientes() {
        return clienteRepository.findAll();
    }

    @Override
    public Cliente consultarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con ID: " + id));
    }

    @Override
    public Cliente consultarClientePorCurp(String curp) {
        return clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con CURP: " + curp));
    }

    @Override
    public Cliente consultarClientePorRfc(String rfc) {
        return clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con RFC: " + rfc));
    }

    @Override
    public Cliente consultarClientePorCorreo(String correo) {
        return clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente no encontrado con correo: " + correo));
    }

    @Override
    public Cuenta consultarCuentaPorNumero(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException("Cuenta bancaria no encontrada: " + numeroCuenta));
    }

    @Override
    public List<Cliente> consultarClientesActivos() {
        return clienteRepository.findByActivoTrue();
    }

    @Override
    public List<Cuenta> consultarCuentasActivas() {
        return cuentaRepository.findByEstatus("ACTIVA");
    }

    @Override
    public List<Cliente> consultarClientesPorRangoFechas(LocalDate inicio, LocalDate fin) {
        return clienteRepository.findClientesByRangoFechas(inicio, fin);
    }

    @Override
    @Transactional
    public Cliente actualizarCliente(Long id, ClienteRegistroRequestDto dto) {
        Cliente cliente = consultarClientePorId(id);

        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellidoPaterno(dto.getApellidoPaterno());
        cliente.setApellidoMaterno(dto.getApellidoMaterno());
        cliente.setSexo(dto.getSexo());
        cliente.setNacionalidad(dto.getNacionalidad());
        cliente.setEstadoCivil(dto.getEstadoCivil());
        cliente.setCorreo(dto.getCorreo());
        cliente.setTelefonoMovil(dto.getTelefonoMovil());
        cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());
        cliente.setOcupacion(dto.getOcupacion());
        cliente.setEmpresa(dto.getEmpresa());
        cliente.setIngresoMensual(dto.getIngresoMensual());

        if (cliente.getDomicilio() != null && dto.getDomicilio() != null) {
            Domicilio dom = cliente.getDomicilio();
            dom.setCalle(dto.getDomicilio().getCalle());
            dom.setNumeroExterior(dto.getDomicilio().getNumeroExterior());
            dom.setNumeroInterior(dto.getDomicilio().getNumeroInterior());
            dom.setColonia(dto.getDomicilio().getColonia());
            dom.setMunicipio(dto.getDomicilio().getMunicipio());
            dom.setEstado(dto.getDomicilio().getEstado());
            dom.setCodigoPostal(dto.getDomicilio().getCodigoPostal());
            dom.setPais(dto.getDomicilio().getPais());
        }

        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional
    public void bajaLogicaCliente(Long id) {
        Cliente cliente = consultarClientePorId(id);
        cliente.setActivo(false);
        if (cliente.getCuenta() != null) {
            cliente.getCuenta().setEstatus("INACTIVA");
        }
        clienteRepository.save(cliente);
    }

    private String generarNumeroCuentaUnico() {
        String numero;
        do {
            numero = String.format("%010d", (long) (Math.random() * 1_000_000_0000L));
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }
}