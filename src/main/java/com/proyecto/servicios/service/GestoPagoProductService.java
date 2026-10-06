package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;

public interface GestoPagoProductService {

    /**
     * Consulta el catalogo de productos en GestoPago.
     *
     * @throws com.proyecto.servicios.exception.GestoPagoIntegrationException ante errores de integracion
     */
    GestoPagoProductListResponse obtenerProductos();
}
