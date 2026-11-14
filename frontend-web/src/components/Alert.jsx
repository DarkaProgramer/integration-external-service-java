const ICONOS = { error: '!', exito: '✓', info: 'i', aviso: '!' };

export default function Alert({ tipo = 'info', titulo, children, accion }) {
  if (!children && !titulo) return null;
  return (
    <div className={`alerta alerta-${tipo}`} role={tipo === 'error' ? 'alert' : 'status'}>
      <span className="alerta-icono" aria-hidden>{ICONOS[tipo]}</span>
      <div className="alerta-cuerpo">
        {titulo && <strong>{titulo}</strong>}
        {children && <div>{children}</div>}
      </div>
      {accion}
    </div>
  );
}

/** Muestra un ApiError con sus detalles por campo (si los hay). */
export function ErrorApi({ error, onReintentar }) {
  if (!error) return null;
  const detalles = error.detalles ? Object.entries(error.detalles) : [];
  return (
    <Alert
      tipo="error"
      titulo={error.message}
      accion={onReintentar && <button className="btn btn-texto" onClick={onReintentar}>Reintentar</button>}
    >
      {detalles.length > 0 && (
        <ul className="lista-detalles">
          {detalles.map(([campo, msg]) => (
            <li key={campo}>{msg}</li>
          ))}
        </ul>
      )}
    </Alert>
  );
}
