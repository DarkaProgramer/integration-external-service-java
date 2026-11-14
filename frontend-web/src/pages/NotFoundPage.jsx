import { Link } from 'react-router-dom';
import EmptyState from '../components/EmptyState';

export default function NotFoundPage() {
  return (
    <div className="pantalla-centro">
      <EmptyState icono="404" titulo="Página no encontrada" accion={<Link className="btn btn-primario" to="/">Ir al inicio</Link>} />
    </div>
  );
}
