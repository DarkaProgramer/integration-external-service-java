package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 403: el cliente fue dado de baja. */
public class ClienteInactivoException extends PagoException {

    public ClienteInactivoException() {
        super(HttpStatus.FORBIDDEN, "Cliente inactivo", "El cliente no está habilitado para realizar operaciones");
    }
}
