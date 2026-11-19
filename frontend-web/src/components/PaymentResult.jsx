import DetailList from './DetailList';
import ProductLogo from './ProductLogo';
import StatusBadge from './StatusBadge';
import { formatoFecha, formatoMoneda } from '../utils/formato';

const ESTILO = {
  EXITOSO: { icono: '✓', titulo: '¡Pago exitoso!', clase: 'exito' },
  RECHAZADO: { icono: '✕', titulo: 'Pago rechazado', clase: 'error' },
  PENDIENTE: { icono: '⏳', titulo: 'Pago en proceso', clase: 'aviso' },
};

/** Comprobante: se usa al terminar un pago y en el detalle del historial. */
export default function PaymentResult({ pago, children }) {
  const e = ESTILO[pago.resultado] ?? ESTILO.PENDIENTE;
  return (
    <div className="tarjeta comprobante">
      <div className={`resultado-icono resultado-${e.clase}`} aria-hidden>{e.icono}</div>
      <h2>{e.titulo}</h2>
      {pago.mensaje && <p className="texto-suave">{pago.mensaje}</p>}
      <div className="comprobante-servicio">
        <ProductLogo nombre={[pago.servicio, pago.producto].filter(Boolean).join(' ')} tamano={48} />
        <strong>{pago.producto || pago.servicio}</strong>
      </div>
      <p className="monto-grande">{formatoMoneda(pago.monto)}</p>
      <DetailList
        filas={[
          ['Estado', <StatusBadge key="b" resultado={pago.resultado} />],
          ['Folio', <code key="f">{pago.folio}</code>],
          ['Folio del proveedor', pago.folioProveedor && <code>{pago.folioProveedor}</code>],
          ['Servicio', pago.servicio],
          ['Producto', pago.producto],
          ['Referencia', pago.referencia],
          ['Forma de pago', pago.tarjeta ? `Tarjeta de débito ${pago.tarjeta} · contado` : null],
          ['Autorización bancaria', pago.autorizacion && <code key="a">{pago.autorizacion}</code>],
          ['Fecha', formatoFecha(pago.fechaOperacion)],
        ]}
      />
      {pago.resultado === 'EXITOSO' && (
        <button type="button" className="btn btn-texto no-imprimir" onClick={() => window.print()}>🖨️ Imprimir o guardar comprobante (PDF)</button>
      )}
      {children}
    </div>
  );
}
