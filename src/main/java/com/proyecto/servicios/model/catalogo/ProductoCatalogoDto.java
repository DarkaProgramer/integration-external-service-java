package com.proyecto.servicios.model.catalogo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Producto tal como lo muestran la web y la app. Todo sale del catálogo vigente de Gestopago. */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductoCatalogoDto {
    /** "idServicio-idProducto". */
    private String identificador;
    private String idServicio;
    private String idProducto;
    private String nombre;
    /** Proveedor del servicio (Telcel, CFE...). */
    private String servicio;
    private String descripcion;
    private String categoria;
    private String logoUrl;
    /** Precio fijo; ausente si el monto lo define el recibo (montoVariable = true). */
    private BigDecimal precio;
    private boolean montoVariable;
    private BigDecimal montoMinimo;
    private BigDecimal montoMaximo;
    private ReferenciaDto referencia;

    @Getter
    @Builder
    public static class ReferenciaDto {
        private int longitudMinima;
        private int longitudMaxima;
        private boolean soloDigitos;
    }
}
