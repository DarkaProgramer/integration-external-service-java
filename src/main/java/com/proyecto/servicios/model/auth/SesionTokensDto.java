package com.proyecto.servicios.model.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Respuesta de registro, login y refresh. El accessToken va en {@code Authorization: Bearer ...};
 * el refreshToken se guarda de forma segura (Keystore / almacenamiento seguro en la app, memoria o
 * cookie segura en web) y solo se envía a /api/auth/refresh.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SesionTokensDto {
    @Builder.Default
    private String tokenType = "Bearer";
    private String accessToken;
    /** Segundos de vigencia del accessToken. */
    private long expiraEn;
    private String refreshToken;
    /** Segundos de vigencia del refreshToken (y de la sesión, si no se usa). */
    private long refreshExpiraEn;
    private String sesionId;
    private PerfilDto perfil;
    /** Otras sesiones abiertas de la cuenta (en login y registro); a ellas se les envió una alerta. */
    private List<SesionDto> otrasSesionesActivas;
}
