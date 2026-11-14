// Mismas reglas que el backend (RegistroRequestDto, DomicilioDto, PasswordReglas) para avisar antes de enviar.
export const PATRONES = {
  letras: /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$/,
  letrasOpcional: /^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*$/,
  curp: /^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$/,
  rfc: /^[A-Z&Ñ]{3,4}\d{6}[A-V0-9]{3}$/,
  correo: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
  telefono: /^\d{10}$/,
  cp: /^\d{5}$/,
  password: /^(?=.*\p{L})(?=.*\d).{8,72}$/u,
};

export const reglas = {
  requerido: (m = 'Este campo es obligatorio') => (v) => (v === undefined || v === null || String(v).trim() === '' ? m : null),
  patron: (re, m) => (v) => (v && !re.test(String(v).trim()) ? m : null),
  longitud: (min, max, m) => (v) => (v && (String(v).trim().length < min || String(v).trim().length > max) ? m : null),
  max: (max) => (v) => (v && String(v).length > max ? `Máximo ${max} caracteres` : null),
  fechaPasada: (m = 'Debe ser una fecha pasada') => (v) => (v && new Date(v) >= new Date(new Date().toDateString()) ? m : null),
  positivo: (m = 'Debe ser mayor a cero') => (v) => (v !== '' && v !== undefined && !(Number(v) > 0) ? m : null),
};

/** Aplica un esquema {campo: [reglas]} y devuelve {campo: mensaje} solo con los campos con error. */
export function validar(valores, esquema) {
  const errores = {};
  for (const [campo, lista] of Object.entries(esquema)) {
    for (const regla of lista) {
      const error = regla(valores[campo], valores);
      if (error) {
        errores[campo] = error;
        break;
      }
    }
  }
  return errores;
}

/** Valida una referencia con las reglas del catálogo ({longitudMinima, longitudMaxima, soloDigitos}). */
export function validarReferencia(ref, regla) {
  const v = (ref || '').trim();
  if (!v) return 'Escribe la referencia';
  if (!regla) return null;
  if (regla.soloDigitos && !/^\d+$/.test(v)) return 'La referencia solo admite números';
  const { longitudMinima: min, longitudMaxima: max } = regla;
  if (min && max && min === max && v.length !== min) return `La referencia debe tener ${min} caracteres`;
  if (min && v.length < min) return `La referencia debe tener al menos ${min} caracteres`;
  if (max && v.length > max) return `La referencia debe tener como máximo ${max} caracteres`;
  return null;
}
