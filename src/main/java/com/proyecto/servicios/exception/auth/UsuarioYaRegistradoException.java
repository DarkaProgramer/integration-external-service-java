package com.proyecto.servicios.exception.auth;

import com.proyecto.servicios.exception.pagos.PagoException;
import org.springframework.http.HttpStatus;

/** 409: ya hay credenciales para ese correo. */
public class UsuarioYaRegistradoException extends PagoException {

    public UsuarioYaRegistradoException() {
        super(HttpStatus.CONFLICT, "Usuario ya registrado", "Ya existe un usuario con ese correo");
    }
}
