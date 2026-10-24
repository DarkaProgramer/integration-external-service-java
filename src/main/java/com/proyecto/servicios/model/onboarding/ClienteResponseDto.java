package com.proyecto.servicios.model.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representación del cliente que se devuelve al consumidor. No expone el id interno
 * ni la entidad JPA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponseDto {
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correo;
    private String telefonoMovil;
    private String telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private DomicilioDto domicilio;
    private CuentaResponseDto cuenta;
}
