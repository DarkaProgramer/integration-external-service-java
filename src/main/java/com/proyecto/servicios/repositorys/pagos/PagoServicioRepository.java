package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.EstatusPago;
import com.proyecto.servicios.entity.pagos.PagoServicio;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoServicioRepository extends JpaRepository<PagoServicio, Long> {

    Optional<PagoServicio> findByClienteIdAndIdempotencyKey(Long clienteId, String idempotencyKey);

    Optional<PagoServicio> findByFolioAndClienteId(String folio, Long clienteId);

    Optional<PagoServicio> findByFolio(String folio);

    /** Historial del cliente, más reciente primero (el orden lo da el Pageable). */
    Page<PagoServicio> findByClienteId(Long clienteId, Pageable pageable);

    Page<PagoServicio> findByClienteIdAndEstatusIn(Long clienteId, Collection<EstatusPago> estatus, Pageable pageable);

    /** Bloquea la fila del pago (SELECT ... FOR UPDATE) para aplicar un resultado sin carreras. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PagoServicio p where p.id = :id")
    Optional<PagoServicio> findByIdParaActualizar(@Param("id") Long id);

    /** Pagos con los mismos datos, aún vigentes, en la ventana anti doble envío (p. ej. doble clic con otra llave). */
    @Query("""
            select count(p) > 0 from PagoServicio p
            where p.cuenta.id = :cuentaId and p.idServicio = :idServicio and p.idProducto = :idProducto
              and p.referencia = :referencia and p.monto = :monto
              and p.estatus in :estatus and p.createdAt >= :desde
            """)
    boolean existeSimilarReciente(@Param("cuentaId") Long cuentaId,
                                  @Param("idServicio") String idServicio,
                                  @Param("idProducto") String idProducto,
                                  @Param("referencia") String referencia,
                                  @Param("monto") BigDecimal monto,
                                  @Param("estatus") Collection<EstatusPago> estatus,
                                  @Param("desde") LocalDateTime desde);

    @Query("""
            select coalesce(sum(p.monto), 0) from PagoServicio p
            where p.cuenta.id = :cuentaId and p.estatus in :estatus and p.createdAt >= :desde
            """)
    BigDecimal sumarMontosDesde(@Param("cuentaId") Long cuentaId,
                                @Param("estatus") Collection<EstatusPago> estatus,
                                @Param("desde") LocalDateTime desde);

    /** Candidatos a conciliación: en conciliación, o pendientes "atorados" (p. ej. caída del servidor a mitad del flujo). */
    @Query("""
            select p.id from PagoServicio p
            where p.estatus = com.proyecto.servicios.entity.pagos.EstatusPago.EN_CONCILIACION
               or (p.estatus = com.proyecto.servicios.entity.pagos.EstatusPago.PENDIENTE and p.updatedAt < :pendientesAntesDe)
            order by p.updatedAt
            """)
    List<Long> findIdsParaConciliar(@Param("pendientesAntesDe") LocalDateTime pendientesAntesDe, Pageable pageable);
}
