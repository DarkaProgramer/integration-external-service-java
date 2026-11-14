import { config } from '../config/env';
import { tokenStore } from '../auth/tokenStore';

/** Error de la API con el formato {status, error, mensaje, ruta, detalles}. */
export class ApiError extends Error {
  constructor(status, mensaje, { detalles = null, datos = null } = {}) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
    this.detalles = detalles;
    this.datos = datos;
  }
  get esDeRed() {
    return this.status === 0;
  }
}

const MENSAJES_POR_STATUS = {
  400: 'Revisa los datos enviados.',
  401: 'Tu sesión expiró. Vuelve a iniciar sesión.',
  403: 'No tienes permiso para esta operación.',
  404: 'No se encontró lo que buscabas.',
  409: 'La operación ya se había realizado.',
  429: 'Demasiados intentos. Espera unos minutos.',
  503: 'El servicio no está disponible en este momento. Intenta más tarde.',
};

async function leerCuerpo(resp) {
  const texto = await resp.text();
  if (!texto) return null;
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

let refrescoEnCurso = null;

/** Renueva el accessToken una sola vez aunque varias peticiones reciban 401 al mismo tiempo. */
export function refrescarTokens() {
  if (!refrescoEnCurso) {
    refrescoEnCurso = (async () => {
      const refreshToken = tokenStore.getRefreshToken();
      if (!refreshToken) throw new ApiError(401, MENSAJES_POR_STATUS[401]);
      const resp = await fetch(`${config.apiUrl}/api/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
      const cuerpo = await leerCuerpo(resp);
      if (!resp.ok) throw new ApiError(resp.status, cuerpo?.mensaje || MENSAJES_POR_STATUS[401]);
      tokenStore.guardar(cuerpo.datos);
      return cuerpo.datos;
    })().finally(() => {
      refrescoEnCurso = null;
    });
  }
  return refrescoEnCurso;
}

/**
 * Hace una petición a la API.
 * @param {string} ruta  ruta que empieza con /api
 * @param {object} opciones  method, body, headers, auth (default true),
 *   aceptar: lista de status no-2xx que se devuelven en vez de lanzar error (p. ej. 422 y 202 del pago).
 * @returns {{status:number, cuerpo:object, datos:any, headers:Headers}}
 */
export async function request(ruta, { method = 'GET', body, headers = {}, auth = true, aceptar = [] } = {}, reintento = true) {
  const finalHeaders = { Accept: 'application/json', ...headers };
  if (body !== undefined) finalHeaders['Content-Type'] = 'application/json';
  if (auth) {
    const token = tokenStore.getAccessToken();
    if (token) finalHeaders.Authorization = `Bearer ${token}`;
    else if (reintento && tokenStore.getRefreshToken()) {
      // Recargó la página: no hay accessToken en memoria, pero sí refresh.
      try {
        await refrescarTokens();
      } catch (e) {
        tokenStore.limpiar('expirada');
        throw e;
      }
      return request(ruta, { method, body, headers, auth, aceptar }, false);
    }
  }

  let resp;
  try {
    resp = await fetch(`${config.apiUrl}${ruta}`, {
      method,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'No se pudo conectar con el servidor. Revisa tu conexión (si el servidor estaba dormido, tarda ~1 minuto en despertar).');
  }

  if (resp.status === 401 && auth && reintento) {
    try {
      await refrescarTokens();
    } catch {
      tokenStore.limpiar('expirada');
      throw new ApiError(401, MENSAJES_POR_STATUS[401]);
    }
    return request(ruta, { method, body, headers, auth, aceptar }, false);
  }
  if (resp.status === 401 && auth) {
    tokenStore.limpiar('expirada');
  }

  const cuerpo = await leerCuerpo(resp);
  if (resp.ok || aceptar.includes(resp.status)) {
    return { status: resp.status, cuerpo, datos: cuerpo?.datos, headers: resp.headers };
  }
  throw new ApiError(resp.status, cuerpo?.mensaje || MENSAJES_POR_STATUS[resp.status] || 'Ocurrió un error inesperado.', {
    detalles: cuerpo?.detalles,
    datos: cuerpo?.datos,
  });
}
