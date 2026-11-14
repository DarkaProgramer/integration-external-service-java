const ETIQUETAS = {
  EXITOSO: { texto: 'Exitoso', clase: 'exito' },
  RECHAZADO: { texto: 'Rechazado', clase: 'error' },
  PENDIENTE: { texto: 'Pendiente', clase: 'aviso' },
};

export default function StatusBadge({ resultado }) {
  const e = ETIQUETAS[resultado] ?? { texto: resultado ?? '—', clase: 'neutro' };
  return <span className={`badge badge-${e.clase}`}>{e.texto}</span>;
}
