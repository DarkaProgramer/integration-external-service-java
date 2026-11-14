import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import AuthLayout from './AuthLayout';
import Field from '../components/Field';
import Button from '../components/Button';
import { ErrorApi } from '../components/Alert';
import { PATRONES, reglas, validar } from '../utils/validacion';

const r = reglas;
const nombre = (etq) => [r.requerido(`${etq} es obligatorio`), r.longitud(2, 50, 'Entre 2 y 50 caracteres'), r.patron(PATRONES.letras, 'Solo letras y espacios')];

const PASOS = [
  {
    titulo: 'Datos personales',
    esquema: {
      nombre: nombre('El nombre'),
      segundoNombre: [r.max(50), r.patron(PATRONES.letrasOpcional, 'Solo letras y espacios')],
      apellidoPaterno: nombre('El apellido paterno'),
      apellidoMaterno: nombre('El apellido materno'),
      fechaNacimiento: [r.requerido('La fecha de nacimiento es obligatoria'), r.fechaPasada()],
      sexo: [r.requerido('Selecciona una opción')],
      curp: [r.requerido('La CURP es obligatoria'), r.patron(PATRONES.curp, 'CURP inválida (18 caracteres)')],
      rfc: [r.requerido('El RFC es obligatorio'), r.patron(PATRONES.rfc, 'RFC inválido')],
      nacionalidad: [r.requerido(), r.max(50)],
      estadoCivil: [r.requerido('Selecciona una opción')],
    },
  },
  {
    titulo: 'Contacto y domicilio',
    esquema: {
      telefonoMovil: [r.requerido('El teléfono es obligatorio'), r.patron(PATRONES.telefono, 'Deben ser 10 dígitos')],
      telefonoAlternativo: [r.patron(PATRONES.telefono, 'Deben ser 10 dígitos')],
      calle: [r.requerido(), r.max(100)],
      numeroExterior: [r.requerido(), r.max(20)],
      numeroInterior: [r.max(20)],
      colonia: [r.requerido(), r.max(100)],
      municipio: [r.requerido(), r.max(100)],
      estado: [r.requerido(), r.max(100)],
      codigoPostal: [r.requerido(), r.patron(PATRONES.cp, 'Deben ser 5 dígitos')],
      pais: [r.requerido(), r.max(50)],
    },
  },
  {
    titulo: 'Trabajo y acceso',
    esquema: {
      ocupacion: [r.requerido(), r.max(100)],
      empresa: [r.requerido(), r.max(100)],
      ingresoMensual: [r.requerido('El ingreso es obligatorio'), r.positivo()],
      correo: [r.requerido('El correo es obligatorio'), r.patron(PATRONES.correo, 'Correo inválido'), r.max(100)],
      password: [r.requerido('La contraseña es obligatoria'), r.patron(PATRONES.password, '8 a 72 caracteres, con al menos una letra y un número')],
      confirmar: [r.requerido('Confirma tu contraseña'), (v, todo) => (v !== todo.password ? 'Las contraseñas no coinciden' : null)],
    },
  },
];

const INICIAL = {
  nombre: '', segundoNombre: '', apellidoPaterno: '', apellidoMaterno: '', fechaNacimiento: '', sexo: '', curp: '', rfc: '',
  nacionalidad: 'Mexicana', estadoCivil: '', telefonoMovil: '', telefonoAlternativo: '', calle: '', numeroExterior: '',
  numeroInterior: '', colonia: '', municipio: '', estado: '', codigoPostal: '', pais: 'México', ocupacion: '', empresa: '',
  ingresoMensual: '', correo: '', password: '', confirmar: '',
};

const MAYUSCULAS = new Set(['curp', 'rfc']);
const DOMICILIO = ['calle', 'numeroExterior', 'numeroInterior', 'colonia', 'municipio', 'estado', 'codigoPostal', 'pais'];

function armarSolicitud(v) {
  const limpio = (x) => (typeof x === 'string' ? x.trim() : x);
  const opcional = (x) => (limpio(x) ? limpio(x) : undefined);
  const domicilio = Object.fromEntries(DOMICILIO.map((k) => [k, k === 'numeroInterior' ? opcional(v[k]) : limpio(v[k])]));
  return {
    nombre: limpio(v.nombre), segundoNombre: opcional(v.segundoNombre), apellidoPaterno: limpio(v.apellidoPaterno),
    apellidoMaterno: limpio(v.apellidoMaterno), fechaNacimiento: v.fechaNacimiento, curp: limpio(v.curp), rfc: limpio(v.rfc),
    sexo: v.sexo, nacionalidad: limpio(v.nacionalidad), estadoCivil: v.estadoCivil, correo: limpio(v.correo),
    telefonoMovil: limpio(v.telefonoMovil), telefonoAlternativo: opcional(v.telefonoAlternativo), domicilio,
    ocupacion: limpio(v.ocupacion), empresa: limpio(v.empresa), ingresoMensual: Number(v.ingresoMensual), password: v.password,
  };
}

export default function RegisterPage() {
  const { registro } = useAuth();
  const navigate = useNavigate();
  const [paso, setPaso] = useState(0);
  const [v, setV] = useState(INICIAL);
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => {
    const { name, value } = e.target;
    setV((prev) => ({ ...prev, [name]: MAYUSCULAS.has(name) ? value.toUpperCase() : value }));
    if (errores[name]) setErrores((er) => ({ ...er, [name]: undefined }));
  };
  const campo = (name, label, props = {}) => (
    <Field label={label} name={name} value={v[name]} onChange={cambiar} error={errores[name]} {...props} />
  );

  const siguiente = async (e) => {
    e.preventDefault();
    const errs = validar(v, PASOS[paso].esquema);
    setErrores(errs);
    if (Object.keys(errs).length) return;
    if (paso < PASOS.length - 1) {
      setPaso(paso + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }
    setEnviando(true);
    setError(null);
    try {
      await registro(armarSolicitud(v));
      navigate('/', { replace: true });
    } catch (err) {
      setError(err);
      setEnviando(false);
      // Si el backend marca un campo, regresar al paso donde está.
      const campoConError = Object.keys(err.detalles ?? {})[0]?.replace('domicilio.', '');
      const idx = PASOS.findIndex((p) => campoConError && campoConError in p.esquema);
      if (idx >= 0 && idx !== paso) setPaso(idx);
    }
  };

  return (
    <AuthLayout titulo="Crea tu cuenta" subtitulo={`Paso ${paso + 1} de ${PASOS.length}: ${PASOS[paso].titulo}`} ancho>
      <div className="pasos" aria-hidden>
        {PASOS.map((p, i) => <span key={p.titulo} className={i <= paso ? 'activo' : ''} />)}
      </div>
      <ErrorApi error={error} />
      <form onSubmit={siguiente} noValidate className="formulario">
        {paso === 0 && (
          <div className="rejilla-2">
            {campo('nombre', 'Nombre', { autoComplete: 'given-name' })}
            {campo('segundoNombre', 'Segundo nombre (opcional)')}
            {campo('apellidoPaterno', 'Apellido paterno', { autoComplete: 'family-name' })}
            {campo('apellidoMaterno', 'Apellido materno')}
            {campo('fechaNacimiento', 'Fecha de nacimiento', { type: 'date', max: new Date().toISOString().slice(0, 10) })}
            {campo('sexo', 'Sexo', {
              as: 'select',
              children: (<><option value="">Selecciona…</option><option value="M">Masculino</option><option value="F">Femenino</option></>),
            })}
            {campo('curp', 'CURP', { maxLength: 18, placeholder: 'ABCD001122HDFRRN09' })}
            {campo('rfc', 'RFC', { maxLength: 13, placeholder: 'ABCD001122XX0' })}
            {campo('nacionalidad', 'Nacionalidad')}
            {campo('estadoCivil', 'Estado civil', {
              as: 'select',
              children: (<><option value="">Selecciona…</option>{['Soltero', 'Casado', 'Divorciado', 'Viudo', 'Unión libre'].map((o) => <option key={o}>{o}</option>)}</>),
            })}
          </div>
        )}
        {paso === 1 && (
          <div className="rejilla-2">
            {campo('telefonoMovil', 'Teléfono móvil', { inputMode: 'numeric', maxLength: 10, autoComplete: 'tel-national' })}
            {campo('telefonoAlternativo', 'Teléfono alternativo (opcional)', { inputMode: 'numeric', maxLength: 10 })}
            {campo('calle', 'Calle', { className: 'col-completa', autoComplete: 'address-line1' })}
            {campo('numeroExterior', 'Número exterior')}
            {campo('numeroInterior', 'Número interior (opcional)')}
            {campo('colonia', 'Colonia')}
            {campo('codigoPostal', 'Código postal', { inputMode: 'numeric', maxLength: 5, autoComplete: 'postal-code' })}
            {campo('municipio', 'Municipio o alcaldía')}
            {campo('estado', 'Estado')}
            {campo('pais', 'País')}
          </div>
        )}
        {paso === 2 && (
          <div className="rejilla-2">
            {campo('ocupacion', 'Ocupación')}
            {campo('empresa', 'Empresa')}
            {campo('ingresoMensual', 'Ingreso mensual (MXN)', { type: 'number', min: '0.01', step: '0.01', inputMode: 'decimal' })}
            {campo('correo', 'Correo electrónico', { type: 'email', autoComplete: 'email' })}
            {campo('password', 'Contraseña', { type: 'password', autoComplete: 'new-password', ayuda: '8 a 72 caracteres, con letras y números' })}
            {campo('confirmar', 'Confirmar contraseña', { type: 'password', autoComplete: 'new-password' })}
          </div>
        )}
        <div className="acciones">
          {paso > 0 && <Button type="button" variante="secundario" onClick={() => setPaso(paso - 1)} disabled={enviando}>Atrás</Button>}
          <Button type="submit" cargando={enviando}>{paso < PASOS.length - 1 ? 'Continuar' : 'Crear cuenta'}</Button>
        </div>
      </form>
      <p className="texto-centro">¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
    </AuthLayout>
  );
}
