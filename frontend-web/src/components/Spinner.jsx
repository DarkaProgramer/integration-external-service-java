export default function Spinner({ chico = false, texto }) {
  if (texto) {
    return (
      <div className="cargando" role="status">
        <span className="spinner" />
        <span>{texto}</span>
      </div>
    );
  }
  return <span className={`spinner ${chico ? 'spinner-chico' : ''}`} role="status" aria-label="Cargando" />;
}
