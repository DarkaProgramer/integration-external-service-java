import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/authApi';
import { refrescarTokens } from '../api/http';
import { tokenStore } from './tokenStore';

const AuthContext = createContext(null);

const MOTIVOS = {
  expirada: 'Tu sesión expiró o se cerró desde otro dispositivo. Inicia sesión de nuevo.',
};

export function AuthProvider({ children }) {
  const [estado, setEstado] = useState('cargando'); // cargando | autenticado | anonimo
  const [perfil, setPerfil] = useState(null);
  const [aviso, setAviso] = useState(null); // mensaje para la pantalla de login
  const [otrasSesiones, setOtrasSesiones] = useState([]);

  // Al abrir la página: si hay refreshToken en esta pestaña, recuperar la sesión.
  useEffect(() => {
    let activo = true;
    (async () => {
      if (!tokenStore.getRefreshToken()) {
        setEstado('anonimo');
        return;
      }
      try {
        const datos = await refrescarTokens();
        if (!activo) return;
        setPerfil(datos.perfil ?? (await authApi.perfil()));
        setEstado('autenticado');
      } catch {
        tokenStore.limpiar('silencioso');
        if (activo) setEstado('anonimo');
      }
    })();
    return () => {
      activo = false;
    };
  }, []);

  useEffect(
    () =>
      tokenStore.alPerderSesion((motivo) => {
        setPerfil(null);
        setEstado('anonimo');
        if (MOTIVOS[motivo]) setAviso(MOTIVOS[motivo]);
      }),
    [],
  );

  const iniciar = useCallback((datos) => {
    tokenStore.guardar(datos);
    setPerfil(datos.perfil);
    setOtrasSesiones(datos.otrasSesionesActivas ?? []);
    setAviso(null);
    setEstado('autenticado');
  }, []);

  const login = useCallback(async (correo, password) => iniciar(await authApi.login(correo, password)), [iniciar]);
  const registro = useCallback(async (datos) => iniciar(await authApi.registro(datos)), [iniciar]);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      /* aunque falle la red, la sesión local se borra */
    }
    tokenStore.limpiar('logout');
  }, []);

  const recargarPerfil = useCallback(async () => {
    const p = await authApi.perfil();
    setPerfil(p);
    return p;
  }, []);

  const valor = useMemo(
    () => ({
      estado, perfil, aviso, otrasSesiones, login, registro, logout, recargarPerfil,
      limpiarOtrasSesiones: () => setOtrasSesiones([]),
    }),
    [estado, perfil, aviso, otrasSesiones, login, registro, logout, recargarPerfil],
  );
  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de <AuthProvider>');
  return ctx;
}
