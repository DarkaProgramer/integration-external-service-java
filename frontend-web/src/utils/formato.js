const moneda = new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' });
const fecha = new Intl.DateTimeFormat('es-MX', { dateStyle: 'medium', timeStyle: 'short' });

export const formatoMoneda = (v) => (v === null || v === undefined || v === '' ? '—' : moneda.format(Number(v)));
export const formatoFecha = (v) => {
  if (!v) return '—';
  const d = new Date(v);
  return Number.isNaN(d.getTime()) ? String(v) : fecha.format(d);
};
export const iniciales = (texto = '') =>
  texto.split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase() || '?';

export function nuevaIdempotencyKey() {
  if (crypto?.randomUUID) return crypto.randomUUID();
  // Respaldo para navegadores viejos (UUID v4 con crypto.getRandomValues).
  const b = crypto.getRandomValues(new Uint8Array(16));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  const h = [...b].map((x) => x.toString(16).padStart(2, '0')).join('');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}
