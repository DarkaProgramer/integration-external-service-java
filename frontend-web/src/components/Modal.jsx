import { useEffect } from 'react';

export default function Modal({ abierto, titulo, onCerrar, children, pie }) {
  useEffect(() => {
    if (!abierto) return undefined;
    const esc = (e) => e.key === 'Escape' && onCerrar?.();
    document.addEventListener('keydown', esc);
    return () => document.removeEventListener('keydown', esc);
  }, [abierto, onCerrar]);

  if (!abierto) return null;
  return (
    <div className="modal-fondo" onClick={onCerrar}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-titulo" onClick={(e) => e.stopPropagation()}>
        <header className="modal-cabecera">
          <h2 id="modal-titulo">{titulo}</h2>
          {onCerrar && <button className="icono-btn" onClick={onCerrar} aria-label="Cerrar">×</button>}
        </header>
        <div className="modal-cuerpo">{children}</div>
        {pie && <footer className="modal-pie">{pie}</footer>}
      </div>
    </div>
  );
}
