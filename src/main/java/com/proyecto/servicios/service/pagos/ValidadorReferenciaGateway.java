package com.proyecto.servicios.service.pagos;

/** Puerto hacia el proveedor para comprobar que una referencia existe antes de cobrar. */
public interface ValidadorReferenciaGateway {

    ResultadoValidacionReferencia validar(ProductoCatalogo producto, String referencia);
}
