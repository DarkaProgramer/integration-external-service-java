package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.ValidacionReferenciaResponseDto;
import com.proyecto.servicios.model.pagos.ValidarReferenciaRequestDto;
import com.proyecto.servicios.seguridad.ClienteAutenticado;

/**
 * Validación de referencias con Gestopago. El pago solo se habilita con un comprobante de validación vigente.
 */
public interface ValidacionReferenciaService {

    ValidacionReferenciaResponseDto validar(ClienteAutenticado cliente, ValidarReferenciaRequestDto solicitud);

    /**
     * Comprueba que el pago traiga una validación vigente, sin usar, del mismo cliente, producto y referencia.
     *
     * @throws com.proyecto.servicios.exception.pagos.ReferenciaNoValidadaException si no la hay o no coincide
     * @throws com.proyecto.servicios.exception.pagos.ValidacionYaUsadaException    si ya se usó en otro pago
     */
    void verificarParaPago(ClienteAutenticado cliente, PagoServicioRequestDto solicitud);

}
