package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.TarjetaGuardada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TarjetaGuardadaRepository extends JpaRepository<TarjetaGuardada, Long> {

    List<TarjetaGuardada> findByClienteIdAndActivaTrueOrderByCreatedAtDesc(Long clienteId);

    Optional<TarjetaGuardada> findByTokenAndClienteIdAndActivaTrue(String token, Long clienteId);
}
