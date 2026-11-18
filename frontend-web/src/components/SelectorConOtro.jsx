import { useState } from 'react';
import Field from './Field';

const OTRO = '__otro__';

/**
 * Lista desplegable con la opción "Otro (escribir)…". Si el valor no está en la lista
 * (o no hay lista), muestra un campo de texto para escribirlo a mano.
 * Cuando cambian las opciones (otro país u otro estado), el padre le da otra `key` para reiniciarlo.
 */
export default function SelectorConOtro({ label, name, value, opciones = [], onChange, error, cargando = false, placeholder, disabled }) {
  const enLista = opciones.includes(value);
  const [manual, setManual] = useState(Boolean(value) && !enLista);

  if (!opciones.length || manual) {
    return (
      <Field
        label={label}
        name={name}
        value={value}
        onChange={(e) => onChange(name, e.target.value)}
        error={error}
        disabled={disabled}
        placeholder={cargando ? 'Cargando lista…' : placeholder ?? 'Escríbelo'}
        ayuda={opciones.length ? (
          <>
            ¿Está en la lista?{' '}
            <button type="button" className="btn-texto btn-enlace" onClick={() => { setManual(false); onChange(name, ''); }}>
              Elegir de la lista
            </button>
          </>
        ) : undefined}
      />
    );
  }

  return (
    <Field
      as="select"
      label={label}
      name={name}
      value={enLista ? value : ''}
      error={error}
      disabled={disabled}
      ayuda="¿No aparece? Elige “Otro (escribir)” al final."
      onChange={(e) => {
        if (e.target.value === OTRO) {
          setManual(true);
          onChange(name, '');
        } else {
          onChange(name, e.target.value);
        }
      }}
    >
      <option value="">Selecciona…</option>
      {opciones.map((o) => <option key={o} value={o}>{o}</option>)}
      <option value={OTRO}>Otro (escribir)…</option>
    </Field>
  );
}
