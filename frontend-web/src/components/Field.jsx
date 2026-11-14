import { useId } from 'react';

/** Campo de formulario con etiqueta, ayuda y mensaje de error accesible. */
export default function Field({ label, error, ayuda, as = 'input', children, className = '', ...props }) {
  const id = useId();
  const Control = as;
  const descripcion = error ? `${id}-error` : ayuda ? `${id}-ayuda` : undefined;
  return (
    <div className={`campo ${error ? 'campo-error' : ''} ${className}`}>
      {label && <label htmlFor={id}>{label}</label>}
      <Control id={id} aria-invalid={Boolean(error)} aria-describedby={descripcion} {...props}>
        {children}
      </Control>
      {error ? (
        <small id={`${id}-error`} className="campo-mensaje error">{error}</small>
      ) : ayuda ? (
        <small id={`${id}-ayuda`} className="campo-mensaje">{ayuda}</small>
      ) : null}
    </div>
  );
}
