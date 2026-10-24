package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Criterios de búsqueda dinámica de clientes. Todos los filtros son opcionales y se combinan
 * con AND; si no se envía ninguno se devuelven todos los clientes.
 * Los filtros de texto (nombre, apellidos, municipio, estado) buscan coincidencias parciales
 * sin distinguir mayúsculas; el resto busca coincidencia exacta.
 */
@Data
public class ClienteConsultaRequestDto {

    @Size(max = 50, message = "El nombre no puede exceder los 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS_OPCIONAL, message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El apellido paterno no puede exceder los 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS_OPCIONAL, message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @Size(max = 50, message = "El apellido materno no puede exceder los 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS_OPCIONAL, message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @Pattern(regexp = ValidacionPatrones.CURP, message = "Formato de CURP inválido")
    private String curp;

    @Pattern(regexp = ValidacionPatrones.RFC, message = "Formato de RFC inválido")
    private String rfc;

    @Email(regexp = ValidacionPatrones.CORREO, message = "Debe proporcionar un correo electrónico válido")
    private String correo;

    @Pattern(regexp = ValidacionPatrones.TELEFONO, message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = ValidacionPatrones.SEXO, message = "El sexo debe ser M (masculino) o F (femenino)")
    private String sexo;

    private Boolean activo;

    @Pattern(regexp = ValidacionPatrones.ESTATUS_CUENTA, message = "El estatus de la cuenta debe ser ACTIVA o INACTIVA")
    private String estatusCuenta;

    @Size(max = 100, message = "El municipio no puede exceder los 100 caracteres")
    private String municipio;

    @Size(max = 100, message = "El estado no puede exceder los 100 caracteres")
    private String estado;

    @Pattern(regexp = ValidacionPatrones.CODIGO_POSTAL, message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @DecimalMin(value = "0.00", message = "El ingreso mínimo no puede ser negativo")
    private BigDecimal ingresoMinimo;

    @DecimalMin(value = "0.00", message = "El ingreso máximo no puede ser negativo")
    private BigDecimal ingresoMaximo;

    @PastOrPresent(message = "La fecha de registro inicial no puede ser futura")
    private LocalDate fechaRegistroDesde;

    private LocalDate fechaRegistroHasta;

    @JsonIgnore
    @AssertTrue(message = "La fecha de registro inicial no puede ser posterior a la fecha final")
    public boolean isRangoFechasValido() {
        return fechaRegistroDesde == null || fechaRegistroHasta == null || !fechaRegistroDesde.isAfter(fechaRegistroHasta);
    }

    @JsonIgnore
    @AssertTrue(message = "El ingreso mínimo no puede ser mayor que el ingreso máximo")
    public boolean isRangoIngresosValido() {
        return ingresoMinimo == null || ingresoMaximo == null || ingresoMinimo.compareTo(ingresoMaximo) <= 0;
    }
}
