package com.proyecto.servicios.service.pagos;

import java.math.BigDecimal;

public interface CatalogoProductosService {

    /**
     * Verifica que la combinación servicio/producto exista en el catálogo del proveedor y que el monto
     * corresponda con su precio o con el rango permitido.
     *
     * @return el producto del catálogo (fuente de verdad del nombre y precio)
     * @throws com.proyecto.servicios.exception.pagos.ProductoNoDisponibleException si no existe
     * @throws com.proyecto.servicios.exception.pagos.MontoNoPermitidoException      si el monto no corresponde
     * @throws com.proyecto.servicios.exception.pagos.CatalogoNoDisponibleException si no se pudo consultar el catálogo
     */
    ProductoCatalogo validarProductoYMonto(String idServicio, String idProducto, BigDecimal monto);
}
