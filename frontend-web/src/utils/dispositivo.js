/** Describe este navegador para que el backend nombre la sesión (p. ej. "Chrome en Windows"). */
export function dispositivoActual() {
  const ua = navigator.userAgent;
  const navegador = /Edg\//.test(ua) ? 'Edge' : /OPR\//.test(ua) ? 'Opera' : /Firefox\//.test(ua) ? 'Firefox'
    : /Chrome\//.test(ua) ? 'Chrome' : /Safari\//.test(ua) ? 'Safari' : 'Navegador';
  const so = /Windows/.test(ua) ? 'Windows' : /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS'
    : /Mac OS X/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : '';
  return { tipo: 'WEB', nombre: so ? `${navegador} en ${so}` : navegador };
}
