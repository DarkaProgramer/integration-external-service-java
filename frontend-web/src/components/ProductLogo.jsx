import { useState } from 'react';
import { iniciales } from '../utils/formato';

const COLORES = ['#4f46e5', '#0891b2', '#059669', '#d97706', '#db2777', '#7c3aed', '#dc2626', '#2563eb'];

/** Logo del servicio; si no hay URL o la imagen falla, muestra las iniciales sobre un color estable. */
export default function ProductLogo({ url, nombre = '', tamano = 48 }) {
  const [fallo, setFallo] = useState(false);
  const color = COLORES[[...nombre].reduce((a, c) => a + c.charCodeAt(0), 0) % COLORES.length];
  const estilo = { width: tamano, height: tamano };
  if (url && !fallo) {
    return <img className="logo" src={url} alt={nombre} style={estilo} loading="lazy" onError={() => setFallo(true)} />;
  }
  return (
    <span className="logo logo-iniciales" style={{ ...estilo, background: color, fontSize: tamano * 0.36 }} aria-label={nombre}>
      {iniciales(nombre)}
    </span>
  );
}
