import Spinner from './Spinner';

export default function Button({ variante = 'primario', cargando = false, bloque = false, children, className = '', ...props }) {
  return (
    <button
      className={`btn btn-${variante} ${bloque ? 'btn-bloque' : ''} ${className}`}
      disabled={cargando || props.disabled}
      {...props}
    >
      {cargando && <Spinner chico />}
      <span>{children}</span>
    </button>
  );
}
