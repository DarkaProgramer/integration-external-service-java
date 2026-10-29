package com.proyecto.servicios.exception.pagos;

import org.springframework.http.HttpStatus;

/** 401: falta el token del cliente, es inválido o expiró. No se indica cuál de los casos para no orientar a un atacante. */
public class AutenticacionClienteException extends PagoException {

    public AutenticacionClienteException() {
        super(HttpStatus.UNAUTHORIZED, "No autenticado", "Se requiere un token de acceso válido");
    }
}
