package com.proyecto.servicios.repositorys.pagos;

import com.proyecto.servicios.entity.pagos.MovimientoCuenta;
import com.proyecto.servicios.entity.pagos.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoCuentaRepository extends JpaRepository<MovimientoCuenta, Long> {

    boolean existsByPagoIdAndTipo(Long pagoId, TipoMovimiento tipo);

    List<MovimientoCuenta> findByPagoIdOrderByIdAsc(Long pagoId);
}
