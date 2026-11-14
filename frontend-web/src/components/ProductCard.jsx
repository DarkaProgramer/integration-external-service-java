import { Link } from 'react-router-dom';
import ProductLogo from './ProductLogo';
import { formatoMoneda } from '../utils/formato';

export default function ProductCard({ producto }) {
  const { identificador, nombre, servicio, descripcion, categoria, logoUrl, precio, montoVariable, montoMinimo, montoMaximo } = producto;
  return (
    <Link to={`/pagar/${encodeURIComponent(identificador)}`} className="tarjeta tarjeta-producto">
      <div className="producto-cabecera">
        <ProductLogo url={logoUrl} nombre={servicio || nombre} />
        {categoria && <span className="chip chip-chico">{categoria}</span>}
      </div>
      <h3>{nombre}</h3>
      <p className="texto-suave texto-chico">{descripcion || servicio}</p>
      <div className="producto-precio">
        {montoVariable
          ? <span>{montoMinimo || montoMaximo ? `${formatoMoneda(montoMinimo)} – ${formatoMoneda(montoMaximo)}` : 'Monto variable'}</span>
          : <span>{formatoMoneda(precio)}</span>}
        <span className="flecha" aria-hidden>→</span>
      </div>
    </Link>
  );
}
