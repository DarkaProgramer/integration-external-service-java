package com.proyecto.servicios.service.Impl.pagos;

import com.proyecto.servicios.entity.pagos.PagoServicio;
import com.proyecto.servicios.model.pagos.PagoServicioResponseDto;
import com.proyecto.servicios.model.pagos.ResultadoPago;
import org.springframework.stereotype.Component;

@Component
public class PagoServicioMapper {

    private static final int DIGITOS_VISIBLES = 4;

    public PagoServicioResponseDto toDto(PagoServicio pago) {
        return PagoServicioResponseDto.builder()
                .folio(pago.getFolio())
                .estatus(pago.getEstatus())
                .resultado(ResultadoPago.de(pago.getEstatus()))
                .mensaje(ResultadoPago.mensaje(pago.getEstatus()))
                .idServicio(pago.getIdServicio())
                .idProducto(pago.getIdProducto())
                .producto(pago.getNombreProducto())
                .servicio(pago.getNombreServicio())
                .referencia(enmascarar(pago.getReferencia()))
                .monto(pago.getMonto())
                .folioProveedor(pago.getFolioProveedor())
                .fechaOperacion(pago.getCreatedAt())
                .build();
    }

    /** Deja visibles solo los últimos 4 caracteres: "5512345678" → "******5678". */
    static String enmascarar(String valor) {
        if (valor == null || valor.length() <= DIGITOS_VISIBLES) {
            return valor;
        }
        return "*".repeat(valor.length() - DIGITOS_VISIBLES) + valor.substring(valor.length() - DIGITOS_VISIBLES);
    }
}
