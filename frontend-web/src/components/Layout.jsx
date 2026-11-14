import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useAlertasSesion } from '../hooks/useAlertasSesion';
import SessionAlertModal from './SessionAlertModal';
import Alert from './Alert';
import { formatoMoneda } from '../utils/formato';

export default function Layout() {
  const { perfil, logout, estado, otrasSesiones, limpiarOtrasSesiones } = useAuth();
  const { alertas, descartar, cerrarSesionRemota } = useAlertasSesion(estado === 'autenticado');
  const [menu, setMenu] = useState(false);
  const [saliendo, setSaliendo] = useState(false);
  const navigate = useNavigate();

  const salir = async () => {
    setSaliendo(true);
    await logout();
    navigate('/login', { replace: true });
  };

  const enlaces = [
    ['/', 'Pagar servicios'],
    ['/historial', 'Historial'],
    ['/cuenta', 'Mi cuenta'],
  ];

  return (
    <div className="app">
      <header className="barra">
        <div className="barra-contenido">
          <NavLink to="/" className="marca" onClick={() => setMenu(false)}>
            <span className="marca-logo" aria-hidden>H</span> Hasani Pagos
          </NavLink>
          <button className="icono-btn menu-btn" aria-label="Menú" aria-expanded={menu} onClick={() => setMenu((m) => !m)}>☰</button>
          <nav className={`nav ${menu ? 'nav-abierta' : ''}`}>
            {enlaces.map(([to, texto]) => (
              <NavLink key={to} to={to} end={to === '/'} onClick={() => setMenu(false)}>{texto}</NavLink>
            ))}
            <div className="nav-usuario">
              <span className="saldo" title="Saldo disponible">{formatoMoneda(perfil?.saldo)}</span>
              <button className="btn btn-texto" onClick={salir} disabled={saliendo}>{saliendo ? 'Saliendo…' : 'Cerrar sesión'}</button>
            </div>
          </nav>
        </div>
      </header>
      <main className="contenido">
        {otrasSesiones.length > 0 && (
          <Alert
            tipo="aviso"
            titulo={`Tu cuenta también está abierta en ${otrasSesiones.length} dispositivo(s) más.`}
            accion={<button className="btn btn-texto" onClick={limpiarOtrasSesiones}>Ocultar</button>}
          >
            Puedes revisarlas y cerrarlas en <NavLink to="/cuenta" onClick={limpiarOtrasSesiones}>Mi cuenta</NavLink>.
          </Alert>
        )}
        <Outlet />
      </main>
      <SessionAlertModal alertas={alertas} onDescartar={descartar} onCerrarSesion={cerrarSesionRemota} />
    </div>
  );
}
