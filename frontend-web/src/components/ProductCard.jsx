import { Link } from 'react-router-dom';
import ProductLogo from './ProductLogo';
import { formatoMoneda } from '../utils/formato';
import { logoDeMarca } from '../utils/logos';

export default function ProductCard({ producto }) {
  const { identificador, nombre, servicio, descripcion, categoria, logoUrl, precio, montoVariable, montoMinimo, montoMaximo } = producto;
  const marca = servicio || nombre;
  const logo = logoDeMarca(marca, nombre);
  return (
    <Link to={`/pagar/${encodeURIComponent(identificador)}`} className="tarjeta tarjeta-producto" style={{ '--marca': logo?.color ?? 'var(--primario)', '--marca-texto': logo?.texto ?? '#fff' }}>
      <div className="producto-banda" aria-hidden />
      <div className="producto-cabecera">
        <ProductLogo url={logoUrl} nombre={marca} tamano={56} />
        {categoria && <span className="chip chip-chico">{categoria}</span>}
      </div>
      <h3>{nombre}</h3>
      <p className="texto-suave texto-chico producto-descripcion">{descripcion || servicio}</p>
      <div className="producto-precio">
        {montoVariable
          ? <span className="producto-monto"><small>Monto</small>{montoMinimo || montoMaximo ? `${formatoMoneda(montoMinimo)} – ${formatoMoneda(montoMaximo)}` : 'Variable'}</span>
          : <span className="producto-monto"><small>Precio</small>{formatoMoneda(precio)}</span>}
        <span className="producto-pagar">Pagar <span aria-hidden>→</span></span>
      </div>
    </Link>
  );
}
