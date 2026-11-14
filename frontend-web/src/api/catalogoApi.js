import { request } from './http';

// El backend ya guarda una copia de 30 min; aquí se evita volver a pedirlo al navegar entre pantallas.
const VIGENCIA_MS = 5 * 60 * 1000;
let cache = null;

export const catalogoApi = {
  async productos({ forzar = false } = {}) {
    if (!forzar && cache && Date.now() - cache.en < VIGENCIA_MS) return cache.datos;
    const { datos } = await request('/api/catalogo/productos');
    cache = { en: Date.now(), datos: datos ?? [] };
    return cache.datos;
  },
  async producto(identificador) {
    const lista = await catalogoApi.productos();
    return lista.find((p) => p.identificador === identificador) ?? null;
  },
  limpiar() {
    cache = null;
  },
};
