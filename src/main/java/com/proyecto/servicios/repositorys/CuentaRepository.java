package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.client.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByEstatus(String estatus);

    boolean existsByNumeroCuenta(String numeroCuenta);
}