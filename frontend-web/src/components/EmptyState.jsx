export default function EmptyState({ icono = '∅', titulo, children, accion }) {
  return (
    <div className="vacio">
      <div className="vacio-icono" aria-hidden>{icono}</div>
      <h3>{titulo}</h3>
      {children && <p>{children}</p>}
      {accion}
    </div>
  );
}
