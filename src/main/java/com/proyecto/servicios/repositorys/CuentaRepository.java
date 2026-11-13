package com.proyecto.servicios.repositorys;

import com.proyecto.servicios.entity.client.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    List<Cuenta> findByEstatus(String estatus);

    boolean existsByNumeroCuenta(String numeroCuenta);

    /**
     * Lee la cuenta bloqueando su fila (SELECT ... FOR UPDATE) hasta que termine la transacción.
     * Serializa las operaciones concurrentes sobre la misma cuenta: dos pagos simultáneos no pueden
     * leer el mismo saldo y descontarlo dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c join fetch c.cliente where c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaParaActualizar(@Param("numeroCuenta") String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.id = :id")
    Optional<Cuenta> findByIdParaActualizar(@Param("id") Long id);
}