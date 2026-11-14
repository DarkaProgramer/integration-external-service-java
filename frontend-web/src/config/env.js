// Configuración leída de variables de entorno de Vite (VITE_*). Nunca se ponen secretos aquí:
// todo lo que empieza con VITE_ termina en el JavaScript público del navegador.
const url = import.meta.env.VITE_API_URL || 'http://localhost:8081';

export const config = {
  apiUrl: url.replace(/\/+$/, ''),
  alertasIntervaloMs: 20_000,
  historialTamano: 10,
};
