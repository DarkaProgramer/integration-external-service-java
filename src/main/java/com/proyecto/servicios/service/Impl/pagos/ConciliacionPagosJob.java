package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.service.pagos.ConciliacionPagosService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pagos.conciliacion.habilitada", havingValue = "true", matchIfMissing = true)
public class ConciliacionPagosJob {

    private final ConciliacionPagosService conciliacionPagosService;

    /** fixedDelay: una ejecución no empieza hasta que termina la anterior (no se solapan). */
    @Scheduled(fixedDelayString = "${pagos.conciliacion.intervalo-ms:60000}",
            initialDelayString = "${pagos.conciliacion.retraso-inicial-ms:30000}")
    public void ejecutar() {
        conciliacionPagosService.conciliarPendientes();
    }
}
