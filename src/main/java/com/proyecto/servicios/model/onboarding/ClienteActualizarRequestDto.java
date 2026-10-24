package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.stream.Stream;

/**
 * Datos que el cliente puede modificar. El cliente se identifica por CURP o RFC.
 * Los campos controlados por el servidor (id, CURP, RFC, fecha de nacimiento, estatus,
 * cuenta, fechas de registro) no existen en este DTO y por lo tanto no se pueden modificar.
 * Los campos son opcionales: sólo se actualizan los que se envían.
 */
@Data
public class ClienteActualizarRequestDto {

    @NotBlank(message = "El CURP o RFC es obligatorio para identificar al cliente a actualizar")
    @Pattern(regexp = ValidacionPatrones.CURP_O_RFC, message = "Debe proporcionar una CURP (18 caracteres) o un RFC (12 o 13 caracteres) válido")
    private String identificadorFiscal;

    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS, message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Size(max = 50, message = "El segundo nombre no puede exceder los 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS_OPCIONAL, message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS, message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = ValidacionPatrones.SOLO_LETRAS, message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @Pattern(regexp = ValidacionPatrones.SEXO, message = "El sexo debe ser M (masculino) o F (femenino)")
    private String sexo;

    @Size(min = 2, max = 50, message = "La nacionalidad debe tener entre 2 y 50 caracteres")
    private String nacionalidad;

    @Size(min = 2, max = 30, message = "El estado civil debe tener entre 2 y 30 caracteres")
    private String estadoCivil;

    @Email(regexp = ValidacionPatrones.CORREO, message = "Debe proporcionar un correo electrónico válido")
    @Size(max = 100, message = "El correo no puede exceder los 100 caracteres")
    private String correo;

    @Pattern(regexp = ValidacionPatrones.TELEFONO, message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = ValidacionPatrones.TELEFONO, message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    private String telefonoAlternativo;

    @Size(min = 2, max = 100, message = "La ocupación debe tener entre 2 y 100 caracteres")
    private String ocupacion;

    @Size(min = 2, max = 100, message = "La empresa debe tener entre 2 y 100 caracteres")
    private String empresa;

    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual admite hasta 10 enteros y 2 decimales")
    private BigDecimal ingresoMensual;

    @Valid
    private DomicilioDto domicilio;

    @JsonIgnore
    @AssertTrue(message = "Debe enviar al menos un dato a actualizar además del CURP o RFC")
    public boolean isAlgunCampoParaActualizar() {
        return Stream.of(nombre, segundoNombre, apellidoPaterno, apellidoMaterno, sexo, nacionalidad,
                        estadoCivil, correo, telefonoMovil, telefonoAlternativo, ocupacion, empresa,
                        ingresoMensual, domicilio)
                .anyMatch(java.util.Objects::nonNull);
    }
}
