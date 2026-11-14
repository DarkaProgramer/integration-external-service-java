/** Lista de pares etiqueta/valor (resumen de pago, comprobante, detalle). */
export default function DetailList({ filas }) {
  return (
    <dl className="detalles">
      {filas.filter(Boolean).filter(([, v]) => v !== undefined && v !== null && v !== '').map(([etiqueta, valor]) => (
        <div key={etiqueta} className="detalle-fila">
          <dt>{etiqueta}</dt>
          <dd>{valor}</dd>
        </div>
      ))}
    </dl>
  );
}
