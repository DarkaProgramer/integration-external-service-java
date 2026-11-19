import { useState } from 'react';
import { authApi } from '../api/authApi';
import { useAuth } from '../auth/AuthContext';
import { useAsync } from '../hooks/useAsync';
import PageHeader from '../components/PageHeader';
import DetailList from '../components/DetailList';
import Button from '../components/Button';
import Spinner from '../components/Spinner';
import { ErrorApi } from '../components/Alert';
import { formatoFecha } from '../utils/formato';

export default function AccountPage() {
  const { perfil } = useAuth();
  const { datos: sesiones, cargando, error, recargar } = useAsync(() => authApi.sesiones(), []);
  const [cerrando, setCerrando] = useState(null);
  const [errorCerrar, setErrorCerrar] = useState(null);

  const cerrar = async (sesionId) => {
    setCerrando(sesionId);
    setErrorCerrar(null);
    try {
      await authApi.cerrarSesion(sesionId);
      await recargar();
    } catch (e) {
      setErrorCerrar(e);
    } finally {
      setCerrando(null);
    }
  };

  return (
    <>
      <PageHeader titulo="Mi cuenta" />
      <div className="rejilla-cuenta">
        <section className="tarjeta">
          <h2>Perfil</h2>
          <DetailList
            filas={[
              ['Nombre', [perfil?.nombre, perfil?.apellidoPaterno, perfil?.apellidoMaterno].filter(Boolean).join(' ')],
              ['Correo', perfil?.correo],
              ['Teléfono', perfil?.telefonoMovil],
              ['Número de cuenta', perfil?.numeroCuenta],
              ['Estatus', perfil?.estatusCuenta],
            ]}
          />
        </section>
        <section className="tarjeta">
          <h2>Sesiones abiertas</h2>
          <p className="texto-suave texto-chico">Si no reconoces un dispositivo, cierra su sesión.</p>
          <ErrorApi error={error || errorCerrar} onReintentar={error ? recargar : undefined} />
          {cargando && !sesiones && <Spinner texto="Cargando sesiones…" />}
          <ul className="lista-sesiones">
            {(sesiones ?? []).map((s) => (
              <li key={s.sesionId}>
                <span className="sesion-icono" aria-hidden>{s.tipoDispositivo === 'MOVIL' ? '📱' : '💻'}</span>
                <div>
                  <strong>{s.nombreDispositivo || (s.tipoDispositivo === 'MOVIL' ? 'App móvil' : 'Navegador web')}</strong>
                  {s.actual && <span className="badge badge-exito">Este dispositivo</span>}
                  <div className="texto-suave texto-chico">Inicio: {formatoFecha(s.inicio)} · Último uso: {formatoFecha(s.ultimoUso)}</div>
                </div>
                {!s.actual && <Button variante="peligro" cargando={cerrando === s.sesionId} onClick={() => cerrar(s.sesionId)}>Cerrar</Button>}
              </li>
            ))}
          </ul>
        </section>
      </div>
    </>
  );
}
