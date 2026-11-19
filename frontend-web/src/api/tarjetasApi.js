import { request } from './http';

/** Tarjetas de débito guardadas: el servidor solo conserva token, marca, últimos 4 dígitos y vencimiento. */
export const tarjetasApi = {
  listar: () => request('/api/tarjetas').then((r) => r.datos ?? []),
  guardar: (tarjeta) => request('/api/tarjetas', { method: 'POST', body: tarjeta }).then((r) => r.datos),
  eliminar: (id) => request(`/api/tarjetas/${encodeURIComponent(id)}`, { method: 'DELETE' }),
};
