package com.proyecto.servicios.service.auth;

import com.proyecto.servicios.model.auth.AlertaSesionDto;
import com.proyecto.servicios.model.auth.LoginRequestDto;
import com.proyecto.servicios.model.auth.PerfilDto;
import com.proyecto.servicios.model.auth.RefreshRequestDto;
import com.proyecto.servicios.model.auth.RegistroRequestDto;
import com.proyecto.servicios.model.auth.SesionDto;
import com.proyecto.servicios.model.auth.SesionTokensDto;
import com.proyecto.servicios.seguridad.ClienteAutenticado;

import java.util.List;

/**
 * Registro, login y sesiones. Las mismas credenciales sirven en web y en la app; cada dispositivo tiene su propia
 * sesión y pueden estar abiertas a la vez. Cuando la cuenta inicia sesión en un dispositivo nuevo, las sesiones
 * que ya estaban abiertas reciben una alerta.
 */
public interface AuthService {

    SesionTokensDto registrar(RegistroRequestDto solicitud);

    SesionTokensDto login(LoginRequestDto solicitud);

    SesionTokensDto refrescar(RefreshRequestDto solicitud);

    void logout(ClienteAutenticado cliente);

    PerfilDto perfil(ClienteAutenticado cliente);

    List<SesionDto> sesiones(ClienteAutenticado cliente);

    void cerrarSesion(ClienteAutenticado cliente, String sesionId);

    List<AlertaSesionDto> alertasPendientes(ClienteAutenticado cliente);

    int marcarAlertasLeidas(ClienteAutenticado cliente);
}
