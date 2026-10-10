package com.proyecto.servicios.model.onboarding;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de validaciones de Bean Validation sobre los DTOs de onboarding.
 * Usa el Validator de Hibernate directamente — sin levantar Spring.
 */
@DisplayName("ClienteRegistroRequestDto - Validaciones de Bean Validation")
class ClienteRegistroRequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ─────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────

    private ClienteRegistroRequestDto buildDtoValido() {
        DomicilioDto dom = new DomicilioDto();
        dom.setCalle("Av. Revolución");
        dom.setNumeroExterior("45");
        dom.setColonia("Centro");
        dom.setMunicipio("Guadalajara");
        dom.setEstado("Jalisco");
        dom.setCodigoPostal("44100");
        dom.setPais("México");

        ClienteRegistroRequestDto dto = new ClienteRegistroRequestDto();
        dto.setNombre("María");
        dto.setApellidoPaterno("López");
        dto.setApellidoMaterno("Ramírez");
        dto.setFechaNacimiento(LocalDate.of(1995, 3, 10));
        dto.setCurp("LORM950310MJCPZR01");
        dto.setRfc("LORM950310AB1");
        dto.setSexo("F");
        dto.setNacionalidad("Mexicana");
        dto.setEstadoCivil("Soltera");
        dto.setCorreo("maria.lopez@email.com");
        dto.setTelefonoMovil("3312345678");
        dto.setOcupacion("Contadora");
        dto.setEmpresa("FinanzasCo");
        dto.setIngresoMensual(BigDecimal.valueOf(15000));
        dto.setDomicilio(dom);
        return dto;
    }

    private Set<ConstraintViolation<ClienteRegistroRequestDto>> validate(ClienteRegistroRequestDto dto) {
        return validator.validate(dto);
    }

    private boolean hasViolationOnField(Set<ConstraintViolation<ClienteRegistroRequestDto>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().startsWith(field));
    }

    // ─────────────────────────────────────────────
    //  1. Nombre
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: nombre")
    class NombreValidationTests {

        @Test
        @DisplayName("Nombre válido — sin violaciones")
        void nombre_valido_sinViolaciones() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("Ana");
            assertThat(validate(dto)).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Nombre en blanco o nulo — violación @NotBlank")
        void nombre_blanco_violacion(String nombre) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre(nombre);
            assertThat(hasViolationOnField(validate(dto), "nombre")).isTrue();
        }

        @Test
        @DisplayName("Nombre de 1 caracter — viola @Size(min=2)")
        void nombre_unCaracter_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("A");
            assertThat(hasViolationOnField(validate(dto), "nombre")).isTrue();
        }

        @Test
        @DisplayName("Nombre de 51 caracteres — viola @Size(max=50)")
        void nombre_51caracteres_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("A".repeat(51));
            assertThat(hasViolationOnField(validate(dto), "nombre")).isTrue();
        }

        @Test
        @DisplayName("Nombre con números — viola @Pattern")
        void nombre_conNumeros_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("Juan123");
            assertThat(hasViolationOnField(validate(dto), "nombre")).isTrue();
        }

        @Test
        @DisplayName("Nombre con caracteres especiales — viola @Pattern")
        void nombre_conEspeciales_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("Juan@Carlos");
            assertThat(hasViolationOnField(validate(dto), "nombre")).isTrue();
        }

        @Test
        @DisplayName("Nombre con acentos y ñ — válido")
        void nombre_conAcentosYEñe_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("José Ñoño");
            assertThat(hasViolationOnField(validate(dto), "nombre")).isFalse();
        }

        @Test
        @DisplayName("Nombre de exactamente 50 caracteres — válido")
        void nombre_50caracteres_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNombre("A".repeat(50));
            assertThat(hasViolationOnField(validate(dto), "nombre")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  2. Apellidos
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: apellidoPaterno / apellidoMaterno")
    class ApellidosValidationTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Apellido paterno nulo/vacío — violación")
        void apellidoPaterno_nuloVacio_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setApellidoPaterno(valor);
            assertThat(hasViolationOnField(validate(dto), "apellidoPaterno")).isTrue();
        }

        @Test
        @DisplayName("Apellido paterno con números — violación @Pattern")
        void apellidoPaterno_conNumeros_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setApellidoPaterno("García1");
            assertThat(hasViolationOnField(validate(dto), "apellidoPaterno")).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Apellido materno nulo/vacío — violación")
        void apellidoMaterno_nuloVacio_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setApellidoMaterno(valor);
            assertThat(hasViolationOnField(validate(dto), "apellidoMaterno")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  3. Fecha de Nacimiento
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: fechaNacimiento")
    class FechaNacimientoValidationTests {

        @Test
        @DisplayName("Fecha nula — violación @NotNull")
        void fechaNacimiento_nula_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(null);
            assertThat(hasViolationOnField(validate(dto), "fechaNacimiento")).isTrue();
        }

        @Test
        @DisplayName("Fecha futura — viola @Past")
        void fechaNacimiento_futura_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now().plusDays(1));
            assertThat(hasViolationOnField(validate(dto), "fechaNacimiento")).isTrue();
        }

        @Test
        @DisplayName("Fecha de hoy — viola @Past")
        void fechaNacimiento_hoy_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.now());
            assertThat(hasViolationOnField(validate(dto), "fechaNacimiento")).isTrue();
        }

        @Test
        @DisplayName("Fecha pasada válida — sin violaciones")
        void fechaNacimiento_pasada_valida() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setFechaNacimiento(LocalDate.of(1985, 6, 20));
            assertThat(hasViolationOnField(validate(dto), "fechaNacimiento")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  4. CURP
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: curp")
    class CurpValidationTests {

        @Test
        @DisplayName("CURP válida — sin violaciones")
        void curp_valida_sinViolaciones() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCurp("LORM950310MJCPZR01");
            assertThat(hasViolationOnField(validate(dto), "curp")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("CURP nula/vacía — violación")
        void curp_nulaVacia_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCurp(valor);
            assertThat(hasViolationOnField(validate(dto), "curp")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "INVALIDACURP",           // muy corta
                "LORM950310MJCPZR0",      // 17 chars
                "LORM950310MJCPZR012",    // 19 chars
                "lorm950310mjcpzr01",     // minúsculas
                "1ORM950310MJCPZR01",     // empieza con número
                "LORH950310XJCPZR01"      // sexo inválido (X en lugar de H/M)
        })
        @DisplayName("CURP con formato inválido — violación @Pattern")
        void curp_formatoInvalido_violacion(String curp) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCurp(curp);
            assertThat(hasViolationOnField(validate(dto), "curp")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  5. RFC
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: rfc")
    class RfcValidationTests {

        @Test
        @DisplayName("RFC de 13 caracteres (persona física) — válido")
        void rfc_13caracteres_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setRfc("LORM950310AB1");
            assertThat(hasViolationOnField(validate(dto), "rfc")).isFalse();
        }

        @Test
        @DisplayName("RFC de 12 caracteres (persona moral) — válido")
        void rfc_12caracteres_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setRfc("LOR950310AB1");
            assertThat(hasViolationOnField(validate(dto), "rfc")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("RFC nulo/vacío — violación")
        void rfc_nuloVacio_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setRfc(valor);
            assertThat(hasViolationOnField(validate(dto), "rfc")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "LORM9503",            // muy corto
                "LORM95031000AB1EXTRA", // muy largo
                "lorm950310ab1",       // minúsculas
        })
        @DisplayName("RFC con formato inválido — violación @Pattern")
        void rfc_formatoInvalido_violacion(String rfc) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setRfc(rfc);
            assertThat(hasViolationOnField(validate(dto), "rfc")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  6. Correo Electrónico
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: correo")
    class CorreoValidationTests {

        @Test
        @DisplayName("Correo válido — sin violaciones")
        void correo_valido_sinViolaciones() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCorreo("usuario@dominio.com");
            assertThat(hasViolationOnField(validate(dto), "correo")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Correo nulo/vacío — violación")
        void correo_nuloVacio_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCorreo(valor);
            assertThat(hasViolationOnField(validate(dto), "correo")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "sinArroba.com",
                "@sinlocal.com",
                "usuario@",
                "usuario@.com",
                "usuario@dominio",
                "usuario @ dominio.com"
        })
        @DisplayName("Correo con formato inválido — violación @Email")
        void correo_formatoInvalido_violacion(String correo) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setCorreo(correo);
            assertThat(hasViolationOnField(validate(dto), "correo")).isTrue();
        }

        @Test
        @DisplayName("Correo con más de 100 caracteres — violación @Size")
        void correo_masDe100Caracteres_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            String correoLargo = "a".repeat(90) + "@dominio.com"; // > 100 chars
            dto.setCorreo(correoLargo);
            assertThat(hasViolationOnField(validate(dto), "correo")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  7. Teléfono Móvil
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: telefonoMovil")
    class TelefonoMovilValidationTests {

        @Test
        @DisplayName("Teléfono de exactamente 10 dígitos — válido")
        void telefonoMovil_10digitos_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoMovil("3312345678");
            assertThat(hasViolationOnField(validate(dto), "telefonoMovil")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Teléfono nulo/vacío — violación")
        void telefonoMovil_nuloVacio_violacion(String valor) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoMovil(valor);
            assertThat(hasViolationOnField(validate(dto), "telefonoMovil")).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "331234567",        // 9 dígitos
                "33123456789",      // 11 dígitos
                "3312-345678",      // con guion
                "abc1234567",       // con letras
                "+5213312345678"    // con código de país
        })
        @DisplayName("Teléfono con formato inválido — viola @Pattern (exactamente 10 dígitos)")
        void telefonoMovil_formatoInvalido_violacion(String tel) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoMovil(tel);
            assertThat(hasViolationOnField(validate(dto), "telefonoMovil")).isTrue();
        }

        @Test
        @DisplayName("Teléfono alternativo opcional — puede ser nulo")
        void telefonoAlternativo_nulo_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoAlternativo(null);
            assertThat(hasViolationOnField(validate(dto), "telefonoAlternativo")).isFalse();
        }

        @Test
        @DisplayName("Teléfono alternativo con formato inválido — viola @Pattern")
        void telefonoAlternativo_formatoInvalido_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setTelefonoAlternativo("12345");
            assertThat(hasViolationOnField(validate(dto), "telefonoAlternativo")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  8. Ingreso Mensual
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: ingresoMensual")
    class IngresoMensualValidationTests {

        @Test
        @DisplayName("Ingreso mayor a cero — válido")
        void ingresoMensual_positivo_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(BigDecimal.valueOf(0.01));
            assertThat(hasViolationOnField(validate(dto), "ingresoMensual")).isFalse();
        }

        @Test
        @DisplayName("Ingreso nulo — violación @NotNull")
        void ingresoMensual_nulo_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(null);
            assertThat(hasViolationOnField(validate(dto), "ingresoMensual")).isTrue();
        }

        @Test
        @DisplayName("Ingreso de cero — viola @DecimalMin(0.01)")
        void ingresoMensual_cero_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(BigDecimal.ZERO);
            assertThat(hasViolationOnField(validate(dto), "ingresoMensual")).isTrue();
        }

        @Test
        @DisplayName("Ingreso negativo — viola @DecimalMin")
        void ingresoMensual_negativo_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(BigDecimal.valueOf(-1));
            assertThat(hasViolationOnField(validate(dto), "ingresoMensual")).isTrue();
        }

        @Test
        @DisplayName("Ingreso muy grande — válido (sin límite superior)")
        void ingresoMensual_muyGrande_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setIngresoMensual(BigDecimal.valueOf(9_999_999.99));
            assertThat(hasViolationOnField(validate(dto), "ingresoMensual")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  9. Domicilio anidado
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: domicilio (anidado)")
    class DomicilioValidationTests {

        @Test
        @DisplayName("Domicilio nulo — violación @NotNull")
        void domicilio_nulo_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setDomicilio(null);
            assertThat(hasViolationOnField(validate(dto), "domicilio")).isTrue();
        }

        @Test
        @DisplayName("Código postal de 5 dígitos — válido")
        void codigoPostal_5digitos_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCodigoPostal("44100");
            assertThat(hasViolationOnField(validate(dto), "domicilio.codigoPostal")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"4410", "441001", "ABCDE", "4410A"})
        @DisplayName("Código postal con formato inválido — viola @Pattern")
        void codigoPostal_formatoInvalido_violacion(String cp) {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCodigoPostal(cp);
            assertThat(hasViolationOnField(validate(dto), "domicilio.codigoPostal")).isTrue();
        }

        @Test
        @DisplayName("Calle obligatoria — nula viola @NotBlank")
        void calle_nula_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setCalle(null);
            assertThat(hasViolationOnField(validate(dto), "domicilio.calle")).isTrue();
        }

        @Test
        @DisplayName("Número interior opcional — puede ser nulo")
        void numeroInterior_nulo_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.getDomicilio().setNumeroInterior(null);
            assertThat(hasViolationOnField(validate(dto), "domicilio.numeroInterior")).isFalse();
        }
    }

    // ─────────────────────────────────────────────
    //  10. Segundo nombre (opcional)
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campo: segundoNombre (opcional)")
    class SegundoNombreValidationTests {

        @Test
        @DisplayName("Segundo nombre nulo — válido (es opcional)")
        void segundoNombre_nulo_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setSegundoNombre(null);
            assertThat(hasViolationOnField(validate(dto), "segundoNombre")).isFalse();
        }

        @Test
        @DisplayName("Segundo nombre vacío — válido (es opcional)")
        void segundoNombre_vacio_valido() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setSegundoNombre("");
            assertThat(hasViolationOnField(validate(dto), "segundoNombre")).isFalse();
        }

        @Test
        @DisplayName("Segundo nombre con números — viola @Pattern")
        void segundoNombre_conNumeros_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setSegundoNombre("Pedr0");
            assertThat(hasViolationOnField(validate(dto), "segundoNombre")).isTrue();
        }

        @Test
        @DisplayName("Segundo nombre de más de 50 caracteres — viola @Size")
        void segundoNombre_masDe50_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setSegundoNombre("A".repeat(51));
            assertThat(hasViolationOnField(validate(dto), "segundoNombre")).isTrue();
        }
    }

    // ─────────────────────────────────────────────
    //  11. Campos obligatorios faltantes
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("Campos obligatorios faltantes")
    class CamposObligatoriosTests {

        @Test
        @DisplayName("sexo obligatorio — nulo viola @NotBlank")
        void sexo_nulo_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setSexo(null);
            assertThat(hasViolationOnField(validate(dto), "sexo")).isTrue();
        }

        @Test
        @DisplayName("nacionalidad obligatoria — nula viola @NotBlank")
        void nacionalidad_nula_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setNacionalidad(null);
            assertThat(hasViolationOnField(validate(dto), "nacionalidad")).isTrue();
        }

        @Test
        @DisplayName("estadoCivil obligatorio — nulo viola @NotBlank")
        void estadoCivil_nulo_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setEstadoCivil(null);
            assertThat(hasViolationOnField(validate(dto), "estadoCivil")).isTrue();
        }

        @Test
        @DisplayName("ocupacion obligatoria — nula viola @NotBlank")
        void ocupacion_nula_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setOcupacion(null);
            assertThat(hasViolationOnField(validate(dto), "ocupacion")).isTrue();
        }

        @Test
        @DisplayName("empresa obligatoria — nula viola @NotBlank")
        void empresa_nula_violacion() {
            ClienteRegistroRequestDto dto = buildDtoValido();
            dto.setEmpresa(null);
            assertThat(hasViolationOnField(validate(dto), "empresa")).isTrue();
        }

        @Test
        @DisplayName("DTO completamente válido — sin violaciones")
        void dto_completamenteValido_sinViolaciones() {
            assertThat(validate(buildDtoValido())).isEmpty();
        }
    }
}
