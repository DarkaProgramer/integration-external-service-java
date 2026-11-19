// Validaciones de la tarjeta de débito en el navegador (el backend vuelve a validar todo).
// El número y el CVV solo viven en memoria mientras se paga: nunca se guardan.

export const soloDigitos = (v) => (v || '').replace(/\D/g, '');

/** "4000123456759010" → "4000 1234 5675 9010" */
export const formatearNumero = (v) => soloDigitos(v).slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ');

/** "1228" → "12/28" */
export const formatearVencimiento = (v) => {
  const d = soloDigitos(v).slice(0, 4);
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d;
};

export function marca(numero) {
  const n = soloDigitos(numero);
  if (/^4/.test(n)) return 'VISA';
  const dos = Number(n.slice(0, 2));
  const cuatro = Number(n.slice(0, 4));
  if ((dos >= 51 && dos <= 55) || (cuatro >= 2221 && cuatro <= 2720)) return 'MASTERCARD';
  if (/^3[47]/.test(n)) return 'AMEX';
  return null;
}

export function luhnValido(numero) {
  const n = soloDigitos(numero);
  let suma = 0;
  let doble = false;
  for (let i = n.length - 1; i >= 0; i -= 1) {
    let d = Number(n[i]);
    if (doble) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    suma += d;
    doble = !doble;
  }
  return n.length > 0 && suma % 10 === 0;
}

/** Devuelve {campo: mensaje} con los errores de la tarjeta. */
export function validarTarjeta({ numero, titular, vencimiento, cvv }, hoy = new Date()) {
  const errores = {};
  const n = soloDigitos(numero);
  const m = marca(n);
  if (!n) errores.numero = 'Escribe el número de tu tarjeta';
  else if (m === 'AMEX') errores.numero = 'American Express no se acepta: usa una tarjeta de débito Visa o Mastercard';
  else if (!m) errores.numero = 'Solo se aceptan tarjetas de débito Visa o Mastercard';
  else if (n.length !== 16 || !luhnValido(n)) errores.numero = 'El número de tarjeta no es válido';

  const t = (titular || '').trim();
  if (t.length < 5) errores.titular = 'Escribe el nombre como aparece en la tarjeta';
  else if (!/^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ .'-]+$/.test(t)) errores.titular = 'Solo letras';

  const [mm, aa] = (vencimiento || '').split('/');
  const mes = Number(mm);
  const anio = 2000 + Number(aa);
  if (!/^\d{2}\/\d{2}$/.test(vencimiento || '') || mes < 1 || mes > 12) errores.vencimiento = 'Usa el formato MM/AA';
  else if (anio < hoy.getFullYear() || (anio === hoy.getFullYear() && mes < hoy.getMonth() + 1)) errores.vencimiento = 'La tarjeta está vencida';

  if (!/^\d{3}$/.test(cvv || '')) errores.cvv = '3 dígitos al reverso';
  return errores;
}

/** Convierte el formulario al formato que espera la API. */
export function tarjetaParaApi({ numero, titular, vencimiento, cvv }) {
  const [mm, aa] = vencimiento.split('/');
  return { numero: soloDigitos(numero), titular: titular.trim(), mesVencimiento: Number(mm), anioVencimiento: 2000 + Number(aa), cvv };
}
