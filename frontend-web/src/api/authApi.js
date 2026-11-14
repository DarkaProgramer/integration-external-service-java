import { request } from './http';
import { dispositivoActual } from '../utils/dispositivo';

export const authApi = {
  login: (correo, password) =>
    request('/api/auth/login', { method: 'POST', auth: false, body: { correo, password, dispositivo: dispositivoActual() } }).then((r) => r.datos),
  registro: (datos) =>
    request('/api/auth/registro', { method: 'POST', auth: false, body: { ...datos, dispositivo: dispositivoActual() } }).then((r) => r.datos),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  perfil: () => request('/api/auth/perfil').then((r) => r.datos),
  sesiones: () => request('/api/auth/sesiones').then((r) => r.datos ?? []),
  cerrarSesion: (sesionId) => request('/api/auth/sesiones/cerrar', { method: 'POST', body: { sesionId } }),
  alertas: () => request('/api/auth/alertas').then((r) => r.datos ?? []),
  marcarAlertasLeidas: () => request('/api/auth/alertas/leidas', { method: 'POST' }),
};
