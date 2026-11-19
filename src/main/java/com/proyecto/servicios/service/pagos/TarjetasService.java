package com.proyecto.servicios.service.pagos;

import com.proyecto.servicios.model.pagos.PagoServicioRequestDto;
import com.proyecto.servicios.model.pagos.TarjetaDto;
import com.proyecto.servicios.model.pagos.TarjetaGuardadaDto;
import com.proyecto.servicios.seguridad.ClienteAutenticado;

import java.math.BigDecimal;
import java.util.List;

/** Tarjetas de débito del cliente: guardarlas, listarlas, eliminarlas y cobrar con ellas. */
public interface TarjetasService {

    List<TarjetaGuardadaDto> listar(ClienteAutenticado cliente);

    TarjetaGuardadaDto guardar(ClienteAutenticado cliente, TarjetaDto tarjeta);

    void eliminar(ClienteAutenticado cliente, String id);

    /**
     * Cobra el pago de contado con la tarjeta indicada en la solicitud: una guardada (con su CVV) o una nueva
     * (que se guarda si el cliente lo pidió).
     *
     * @throws com.proyecto.servicios.exception.pagos.MedioPagoRequeridoException si no se indicó tarjeta
     */
    AutorizacionTarjeta cobrar(ClienteAutenticado cliente, PagoServicioRequestDto solicitud);

    void reembolsar(AutorizacionTarjeta autorizacion, BigDecimal monto);
}
