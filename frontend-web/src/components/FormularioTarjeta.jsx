import Field from './Field';
import { formatearNumero, formatearVencimiento, marca } from '../utils/tarjeta';

export const TARJETA_VACIA = { numero: '', titular: '', vencimiento: '', cvv: '' };

/** Campos de una tarjeta de débito nueva. Los datos solo viven en memoria mientras se usa el formulario. */
export default function FormularioTarjeta({ tarjeta, errores = {}, onChange, autoFocus = false }) {
  const m = marca(tarjeta.numero);
  return (
    <div className="formulario tarjeta-pago">
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
    </div>
  );
}

export const descripcionTarjeta = (t) => `${t.marca === 'MASTERCARD' ? 'Mastercard' : 'Visa'} •••• ${t.ultimos4}`;
