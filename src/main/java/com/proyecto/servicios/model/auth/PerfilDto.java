package com.proyecto.servicios.model.auth;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** Datos que los frontends necesitan tras iniciar sesión: nombre, contacto y la cuenta con la que se paga. */
@Getter
@Builder
public class PerfilDto {
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String correo;
    private String telefonoMovil;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatusCuenta;
}
