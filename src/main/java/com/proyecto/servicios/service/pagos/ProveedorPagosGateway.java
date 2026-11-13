package com.proyecto.servicios.service.pagos;

import java.util.Optional;

/**
 * Puerto hacia el proveedor de pagos de servicios. El flujo de negocio depende de esta interfaz y no de
 * Gestopago (inversión de dependencias): cambiar de proveedor o ajustar el contrato de sendTx.do solo toca
 * el adaptador.
 * <p>
 * Contrato: las implementaciones <b>no lanzan excepciones</b> por errores de comunicación; siempre clasifican
 * el resultado en {@link ResultadoProveedor.Estado}. Ante cualquier duda deben devolver INDETERMINADA, nunca
 * RECHAZADA, porque RECHAZADA provoca la devolución del dinero al cliente.
 */
public interface ProveedorPagosGateway {

    ResultadoProveedor enviarPago(SolicitudPagoProveedor solicitud);

    /**
     * Consulta al proveedor el resultado de una operación enviada antes (conciliación).
     *
     * @return vacío si el proveedor no ofrece consulta o no fue posible obtener una respuesta concluyente
     */
    Optional<ResultadoProveedor> consultarEstado(SolicitudPagoProveedor solicitud);
}
