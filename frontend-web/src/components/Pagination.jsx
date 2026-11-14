export default function Pagination({ pagina, totalPaginas, onCambiar, deshabilitado }) {
  if (!totalPaginas || totalPaginas <= 1) return null;
  return (
    <nav className="paginacion" aria-label="Paginación">
      <button className="btn btn-secundario" disabled={deshabilitado || pagina === 0} onClick={() => onCambiar(pagina - 1)}>
        ← Anterior
      </button>
      <span>Página {pagina + 1} de {totalPaginas}</span>
      <button className="btn btn-secundario" disabled={deshabilitado || pagina >= totalPaginas - 1} onClick={() => onCambiar(pagina + 1)}>
        Siguiente →
      </button>
    </nav>
  );
}
