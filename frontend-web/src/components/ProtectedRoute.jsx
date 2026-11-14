import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import Spinner from './Spinner';

export function RutaPrivada() {
  const { estado } = useAuth();
  const location = useLocation();
  if (estado === 'cargando') return <div className="pantalla-centro"><Spinner texto="Recuperando tu sesión…" /></div>;
  if (estado === 'anonimo') return <Navigate to="/login" replace state={{ desde: location.pathname }} />;
  return <Outlet />;
}

export function RutaPublica() {
  const { estado } = useAuth();
  if (estado === 'cargando') return <div className="pantalla-centro"><Spinner texto="Cargando…" /></div>;
  if (estado === 'autenticado') return <Navigate to="/" replace />;
  return <Outlet />;
}
