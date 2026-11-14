package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.SesionCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionClienteRepository extends JpaRepository<SesionCliente, String> {

    Optional<SesionCliente> findByRefreshHash(String refreshHash);

    @Query("""
            select s from SesionCliente s
            where s.clienteId = :clienteId and s.cerradaEn is null and s.refreshExpira > :ahora
            order by s.ultimoUso desc
            """)
    List<SesionCliente> findActivas(@Param("clienteId") Long clienteId, @Param("ahora") LocalDateTime ahora);
}
