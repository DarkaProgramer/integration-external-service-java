package com.proyecto.servicios.repositorys.seguridad;

import com.proyecto.servicios.entity.seguridad.AlertaSesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaSesionRepository extends JpaRepository<AlertaSesion, Long> {

    List<AlertaSesion> findBySesionDestinoIdAndLeidaFalseOrderByFechaDesc(String sesionDestinoId);

    @Modifying
    @Query("update AlertaSesion a set a.leida = true where a.sesionDestinoId = :sesionId and a.leida = false")
    int marcarLeidas(@Param("sesionId") String sesionId);
}
