package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteService {
    Cliente registrarCliente(ClienteRegistroRequestDto request);
    List<Cliente> consultarTodosClientes();
    Cliente consultarClientePorId(Long id);
    Cliente consultarClientePorCurp(String curp);
    Cliente consultarClientePorRfc(String rfc);
    Cliente consultarClientePorCorreo(String correo);
    Cuenta consultarCuentaPorNumero(String numeroCuenta);
    List<Cliente> consultarClientesActivos();
    List<Cuenta> consultarCuentasActivas();
    List<Cliente> consultarClientesPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    Cliente actualizarCliente(Long id, ClienteRegistroRequestDto request);
    void bajaLogicaCliente(Long id);
}