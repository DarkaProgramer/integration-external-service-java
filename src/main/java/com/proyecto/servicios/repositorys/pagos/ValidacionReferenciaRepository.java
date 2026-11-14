package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.ValidacionReferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ValidacionReferenciaRepository extends JpaRepository<ValidacionReferencia, String> {

    Optional<ValidacionReferencia> findByIdAndClienteId(String id, Long clienteId);

    /** Consume el comprobante solo si nadie lo usó antes (actualización condicional, segura ante concurrencia). */
    @Modifying
    @Query("update ValidacionReferencia v set v.usadaEn = :ahora, v.folioPago = :folio where v.id = :id and v.usadaEn is null")
    int marcarUsada(@Param("id") String id, @Param("folio") String folio, @Param("ahora") LocalDateTime ahora);
}
