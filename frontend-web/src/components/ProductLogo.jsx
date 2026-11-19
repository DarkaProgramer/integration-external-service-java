import { useState } from 'react';
import { iniciales } from '../utils/formato';
import { logoDeMarca } from '../utils/logos';

const COLORES = ['#4f46e5', '#0891b2', '#059669', '#d97706', '#db2777', '#7c3aed', '#dc2626', '#2563eb'];

/**
 * Logo de la compañía: primero el logo guardado en el proyecto, luego la URL del proveedor
 * y, si ninguno carga, las iniciales sobre un color estable.
 */
export default function ProductLogo({ url, nombre = '', tamano = 48 }) {
  const local = logoDeMarca(nombre)?.url;
  const fuentes = [local, url].filter(Boolean);
  const [intento, setIntento] = useState(0);
  const color = COLORES[[...nombre].reduce((a, c) => a + c.charCodeAt(0), 0) % COLORES.length];
  const estilo = { width: tamano, height: tamano };
  if (intento < fuentes.length) {
    return <img className="logo" src={fuentes[intento]} alt={nombre} style={estilo} loading="lazy" onError={() => setIntento((i) => i + 1)} />;
  }
  return (
    <span className="logo logo-iniciales" style={{ ...estilo, background: color, fontSize: tamano * 0.36 }} aria-label={nombre}>
      {iniciales(nombre)}
    </span>
  );
}
