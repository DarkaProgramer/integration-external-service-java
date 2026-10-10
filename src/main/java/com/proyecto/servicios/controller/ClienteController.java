package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.client.Cliente;
import com.proyecto.servicios.entity.client.Cuenta;
import com.proyecto.servicios.model.onboarding.ClienteRegistroRequestDto;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @PostMapping
    public ResponseEntity<Cliente> registrarCliente(@Valid @RequestBody ClienteRegistroRequestDto request) {
        Cliente nuevoCliente = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCliente);
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> consultarTodos() {
        return ResponseEntity.ok(clienteService.consultarTodosClientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> consultarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.consultarClientePorId(id));
    }

    @GetMapping("/curp/{curp}")
    public ResponseEntity<Cliente> consultarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.consultarClientePorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<Cliente> consultarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.consultarClientePorRfc(rfc));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<Cliente> consultarPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(clienteService.consultarClientePorCorreo(correo));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<Cliente>> consultarClientesActivos() {
        return ResponseEntity.ok(clienteService.consultarClientesActivos());
    }

    @GetMapping("/cuentas/activas")
    public ResponseEntity<List<Cuenta>> consultarCuentasActivas() {
        return ResponseEntity.ok(clienteService.consultarCuentasActivas());
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<Cliente>> consultarPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {

        // Convertimos LocalDate a LocalDateTime para que coincida con el servicio
        LocalDateTime inicioDateTime = inicio.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        return ResponseEntity.ok(clienteService.consultarClientesPorRangoFechas(inicioDateTime, finDateTime));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteRegistroRequestDto request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> bajaLogica(@PathVariable Long id) {
        clienteService.bajaLogicaCliente(id);
        return ResponseEntity.noContent().build();
    }
}