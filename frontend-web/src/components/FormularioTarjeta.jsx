import Field from './Field';
import { formatearNumero, formatearVencimiento, marca, soloDigitos } from '../utils/tarjeta';

export const TARJETA_VACIA = { numero: '', titular: '', vencimiento: '', cvv: '' };

/** Campos de una tarjeta de débito nueva. Los datos solo viven en memoria mientras se usa el formulario. */
const TARJETAS_PRUEBA = [
  ['4000 1234 5675 9010', 'Visa débito: aprobada'],
  ['5200 1234 5675 4321', 'Mastercard débito: aprobada'],
  ['4000 1234 5674 0002', 'Débito: fondos insuficientes'],
  ['4111 1111 1111 1111', 'Crédito: no se acepta'],
];

/** Vista previa de la tarjeta mientras se escribe (como en las pasarelas de pago reales). */
function VistaPrevia({ tarjeta }) {
  const m = marca(tarjeta.numero);
  const digitos = soloDigitos(tarjeta.numero).padEnd(16, '•');
  const numero = digitos.replace(/(.{4})(?=.)/g, '$1 ');
  return (
    <div className={`tarjeta-preview ${m === 'VISA' ? 'visa' : m === 'MASTERCARD' ? 'mastercard' : ''}`} aria-hidden>
      <div className="tp-arriba"><span className="tp-chip" /><span>Débito</span></div>
      <div className="tp-numero">{numero}</div>
      <div className="tp-abajo">
        <div className="tp-titular"><small>Titular</small>{tarjeta.titular || 'NOMBRE APELLIDO'}</div>
        <div><small>Vence</small>{tarjeta.vencimiento || 'MM/AA'}</div>
        <div className="tp-marca">{m === 'VISA' ? 'VISA' : m === 'MASTERCARD' ? 'mastercard' : ''}</div>
      </div>
    </div>
  );
}

export default function FormularioTarjeta({ tarjeta, errores = {}, onChange, autoFocus = false }) {
  const m = marca(tarjeta.numero);
  return (
    <div className="formulario tarjeta-pago">
      <VistaPrevia tarjeta={tarjeta} />
      <Field label={`Número de tarjeta${m && m !== 'AMEX' ? ` · ${m}` : ''}`} value={tarjeta.numero} autoFocus={autoFocus}
        inputMode="numeric" autoComplete="cc-number" placeholder="0000 0000 0000 0000" maxLength={19}
        onChange={(e) => onChange('numero', formatearNumero(e.target.value))} error={errores.numero}
        ayuda="Solo tarjetas de débito Visa o Mastercard" />
      <Field label="Nombre del titular" value={tarjeta.titular} autoComplete="cc-name" placeholder="Como aparece en la tarjeta"
        onChange={(e) => onChange('titular', e.target.value.toUpperCase())} error={errores.titular} />
      <div className="rejilla-2">
        <Field label="Vencimiento" value={tarjeta.vencimiento} inputMode="numeric" autoComplete="cc-exp" placeholder="MM/AA" maxLength={5}
          onChange={(e) => onChange('vencimiento', formatearVencimiento(e.target.value))} error={errores.vencimiento} />
        <Field label="CVV" type="password" value={tarjeta.cvv} inputMode="numeric" autoComplete="cc-csc" placeholder="123" maxLength={3}
          onChange={(e) => onChange('cvv', e.target.value.replace(/\D/g, '').slice(0, 3))} error={errores.cvv} />
      </div>
      <details className="tarjetas-prueba">
        <summary>Tarjetas de prueba (entorno simulado, no se cobra dinero real)</summary>
        <ul>
          {TARJETAS_PRUEBA.map(([n, d]) => (
            <li key={n}><code onClick={() => onChange('numero', n)} title="Usar este número">{n}</code> · {d}</li>
          ))}
        </ul>
        <span className="texto-suave">Usa cualquier titular, una fecha futura y cualquier CVV de 3 dígitos.</span>
      </details>
    </div>
  );
}

export const descripcionTarjeta = (t) => `${t.marca === 'MASTERCARD' ? 'Mastercard' : 'Visa'} •••• ${t.ultimos4}`;
