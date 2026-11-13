package com.proyecto.servicios.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/** Reglas de negocio de los pagos de servicios (prefijo {@code pagos.*} en application.properties). */
@Getter
@Setter
@ConfigurationProperties(prefix = "pagos")
public class PagosProperties {

    /** Rango permitido para productos de monto variable (precio 0 en el catálogo). */
    private BigDecimal montoMinimo = new BigDecimal("1.00");
    private BigDecimal montoMaximo = new BigDecimal("10000.00");
    /** Suma máxima de pagos (no rechazados) por cuenta en el día. */
    private BigDecimal limiteDiario = new BigDecimal("20000.00");
    /** Segundos en los que un pago idéntico (cuenta, producto, referencia y monto) se considera duplicado. */
    private long ventanaDuplicadosSegundos = 120;

    private Catalogo catalogo = new Catalogo();
    private Conciliacion conciliacion = new Conciliacion();

    @Getter
    @Setter
    public static class Catalogo {
        /** Minutos que se reutiliza el catálogo antes de volver a consultarlo al proveedor. */
        private long ttlMinutos = 30;
    }

    @Getter
    @Setter
    public static class Conciliacion {
        private boolean habilitada = true;
        /** Un PENDIENTE sin cambios por más de estos minutos se considera atorado y pasa a conciliación. */
        private long pendienteMinutos = 5;
        /** Intentos de consulta automáticos antes de mandar el pago a revisión manual. */
        private int maxIntentos = 10;
        private int lote = 50;
    }
}
