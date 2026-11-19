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
import { tarjetasApi } from '../api/tarjetasApi';
import { tarjetaParaApi, validarTarjeta } from '../utils/tarjeta';
import Modal from '../components/Modal';
import FormularioTarjeta, { TARJETA_VACIA, descripcionTarjeta } from '../components/FormularioTarjeta';

function MisTarjetas() {
  const { datos: tarjetas, cargando, error, recargar } = useAsync(() => tarjetasApi.listar(), []);
  const [agregando, setAgregando] = useState(false);
  const [nueva, setNueva] = useState(TARJETA_VACIA);
  const [errores, setErrores] = useState({});
  const [guardando, setGuardando] = useState(false);
  const [errorAccion, setErrorAccion] = useState(null);
  const [eliminando, setEliminando] = useState(null);
  const [porEliminar, setPorEliminar] = useState(null);

  const cerrarModal = () => {
    setAgregando(false);
    setNueva(TARJETA_VACIA);
    setErrores({});
    setErrorAccion(null);
  };
  const cambiar = (campo, valor) => {
    setNueva((t) => ({ ...t, [campo]: valor }));
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: undefined }));
  };
  const guardar = async (e) => {
    e?.preventDefault();
    const errs = validarTarjeta(nueva);
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setGuardando(true);
    setErrorAccion(null);
    try {
      await tarjetasApi.guardar(tarjetaParaApi(nueva));
      cerrarModal();
      await recargar();
    } catch (err) {
      setErrorAccion(err);
    } finally {
      setGuardando(false);
    }
  };
  const eliminar = async () => {
    const id = porEliminar.id;
    setEliminando(id);
    setErrorAccion(null);
    try {
      await tarjetasApi.eliminar(id);
      setPorEliminar(null);
      await recargar();
    } catch (err) {
      setErrorAccion(err);
    } finally {
      setEliminando(null);
    }
  };

  return (
    <section className="tarjeta">
      <h2>Mis tarjetas</h2>
      <p className="texto-suave texto-chico">Tarjetas de débito para pagar tus servicios. Solo guardamos la marca, los últimos 4 dígitos y el vencimiento.</p>
      <ErrorApi error={error || (!agregando && errorAccion)} onReintentar={error ? recargar : undefined} />
      {cargando && !tarjetas && <Spinner texto="Cargando tarjetas…" />}
      {tarjetas && tarjetas.length === 0 && <p className="texto-suave">Aún no tienes tarjetas guardadas.</p>}
      <ul className="lista-tarjetas">
        {(tarjetas ?? []).map((t) => (
          <li key={t.id}>
            <span className="tarjeta-icono" aria-hidden>💳</span>
            <div>
              <strong>{descripcionTarjeta(t)}</strong>
              {t.vencida && <span className="badge badge-error">Vencida</span>}
              <div className="texto-suave texto-chico">{t.titular} · Vence {t.vencimiento}</div>
            </div>
            <Button variante="peligro" onClick={() => setPorEliminar(t)}>Eliminar</Button>
          </li>
        ))}
      </ul>
      <Button variante="secundario" onClick={() => setAgregando(true)}>+ Agregar tarjeta</Button>

      <Modal abierto={agregando} titulo="Agregar tarjeta de débito" onCerrar={cerrarModal}
        pie={<><Button variante="secundario" onClick={cerrarModal} disabled={guardando}>Cancelar</Button><Button onClick={guardar} cargando={guardando}>Guardar tarjeta</Button></>}>
        <form onSubmit={guardar} noValidate>
          <FormularioTarjeta tarjeta={nueva} errores={errores} onChange={cambiar} autoFocus />
          <p className="texto-chico texto-suave">🔒 El CVV solo se usa para verificar la tarjeta y no se guarda. Te lo pediremos en cada pago.</p>
          <ErrorApi error={errorAccion} />
          <button type="submit" hidden aria-hidden />
        </form>
      </Modal>

      <Modal abierto={Boolean(porEliminar)} titulo="Eliminar tarjeta" onCerrar={() => setPorEliminar(null)}
        pie={<><Button variante="secundario" onClick={() => setPorEliminar(null)}>Cancelar</Button><Button variante="peligro" onClick={eliminar} cargando={Boolean(eliminando)}>Eliminar</Button></>}>
        {porEliminar && <p>¿Eliminar la tarjeta {descripcionTarjeta(porEliminar)}? Podrás agregarla de nuevo cuando quieras.</p>}
      </Modal>
    </section>
  );
}

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
        <MisTarjetas />
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
