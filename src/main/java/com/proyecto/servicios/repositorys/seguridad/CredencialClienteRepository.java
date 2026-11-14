package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.CredencialCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CredencialClienteRepository extends JpaRepository<CredencialCliente, Long> {

    Optional<CredencialCliente> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
