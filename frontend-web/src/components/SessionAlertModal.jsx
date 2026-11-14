import { useState } from 'react';
import Modal from './Modal';
import Button from './Button';
import { ErrorApi } from './Alert';
import { formatoFecha } from '../utils/formato';

/** Aviso cuando la cuenta inicia sesión en otro dispositivo, con opción de cerrar esa sesión. */
export default function SessionAlertModal({ alertas, onDescartar, onCerrarSesion }) {
  const [ocupado, setOcupado] = useState(null);
  const [error, setError] = useState(null);
  const [cerradas, setCerradas] = useState([]);

  const ejecutar = async (clave, fn) => {
    setOcupado(clave);
    setError(null);
    try {
      await fn();
    } catch (e) {
      setError(e);
    } finally {
      setOcupado(null);
    }
  };

  return (
    <Modal
      abierto={alertas.length > 0}
      titulo="Nuevo inicio de sesión en tu cuenta"
      pie={
        <Button variante="secundario" cargando={ocupado === 'ok'} onClick={() => ejecutar('ok', onDescartar)}>
          Fui yo, entendido
        </Button>
      }
    >
      <p className="texto-suave">Si no reconoces alguno de estos accesos, ciérralo y cambia tu contraseña.</p>
      <ErrorApi error={error} />
      <ul className="lista-alertas">
        {alertas.map((a) => (
          <li key={a.id}>
            <div>
              <strong>{a.nombreDispositivo || (a.tipoDispositivo === 'MOVIL' ? 'App móvil' : 'Navegador web')}</strong>
              <span className="texto-suave"> · {a.tipoDispositivo === 'MOVIL' ? 'Móvil' : 'Web'} · {formatoFecha(a.fecha)}</span>
              {a.mensaje && <p className="texto-chico">{a.mensaje}</p>}
            </div>
            {a.sesionId && !cerradas.includes(a.sesionId) && (
              <Button
                variante="peligro"
                cargando={ocupado === a.id}
                onClick={() =>
                  ejecutar(a.id, async () => {
                    await onCerrarSesion(a.sesionId);
                    setCerradas((c) => [...c, a.sesionId]);
                  })
                }
              >
                Cerrar esa sesión
              </Button>
            )}
          </li>
        ))}
      </ul>
    </Modal>
  );
}
