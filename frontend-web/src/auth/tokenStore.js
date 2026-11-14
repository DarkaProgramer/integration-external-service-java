// Almacenamiento de los tokens de sesión.
// - accessToken: solo en memoria (dura 15 min, se pierde al recargar y se renueva con el refresh).
// - refreshToken: sessionStorage (se borra al cerrar la pestaña; nunca localStorage ni cookies legibles).
const CLAVE = 'hasani.sesion';

let accessToken = null;
const oyentes = new Set();

function leerGuardado() {
  try {
    const crudo = sessionStorage.getItem(CLAVE);
    return crudo ? JSON.parse(crudo) : null;
  } catch {
    return null;
  }
}

export const tokenStore = {
  getAccessToken: () => accessToken,
  getRefreshToken: () => leerGuardado()?.refreshToken ?? null,
  getSesionId: () => leerGuardado()?.sesionId ?? null,

  guardar({ accessToken: access, refreshToken, sesionId }) {
    accessToken = access;
    try {
      sessionStorage.setItem(CLAVE, JSON.stringify({ refreshToken, sesionId }));
    } catch {
      /* almacenamiento bloqueado: la sesión vive solo en memoria */
    }
  },

  limpiar(motivo = 'logout') {
    accessToken = null;
    try {
      sessionStorage.removeItem(CLAVE);
    } catch {
      /* nada que limpiar */
    }
    oyentes.forEach((fn) => fn(motivo));
  },

  /** Avisa cuando la sesión se pierde (logout, refresh vencido, sesión cerrada desde otro dispositivo). */
  alPerderSesion(fn) {
    oyentes.add(fn);
    return () => oyentes.delete(fn);
  },
};
