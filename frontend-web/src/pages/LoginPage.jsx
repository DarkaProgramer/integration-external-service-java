import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import AuthLayout from './AuthLayout';
import Field from '../components/Field';
import Button from '../components/Button';
import Alert, { ErrorApi } from '../components/Alert';
import { PATRONES, reglas, validar } from '../utils/validacion';

const ESQUEMA = {
  correo: [reglas.requerido('Escribe tu correo'), reglas.patron(PATRONES.correo, 'Correo inválido')],
  password: [reglas.requerido('Escribe tu contraseña')],
};

export default function LoginPage() {
  const { login, aviso } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [valores, setValores] = useState({ correo: '', password: '' });
  const [errores, setErrores] = useState({});
  const [error, setError] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const cambiar = (e) => setValores((v) => ({ ...v, [e.target.name]: e.target.value }));

  const enviar = async (e) => {
    e.preventDefault();
    const errs = validar(valores, ESQUEMA);
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setEnviando(true);
    setError(null);
    try {
      await login(valores.correo.trim(), valores.password);
      navigate(location.state?.desde || '/', { replace: true });
    } catch (err) {
      setError(err);
      setEnviando(false);
    }
  };

  return (
    <AuthLayout titulo="Inicia sesión" subtitulo="Entra con tu correo y contraseña.">
      {aviso && !error && <Alert tipo="aviso">{aviso}</Alert>}
      {location.state?.registrado && <Alert tipo="exito">Cuenta creada. Ya puedes iniciar sesión.</Alert>}
      <ErrorApi error={error} />
      <form onSubmit={enviar} noValidate className="formulario">
        <Field label="Correo electrónico" name="correo" type="email" autoComplete="email" value={valores.correo} onChange={cambiar} error={errores.correo} />
        <Field label="Contraseña" name="password" type="password" autoComplete="current-password" value={valores.password} onChange={cambiar} error={errores.password} />
        <Button type="submit" bloque cargando={enviando}>Entrar</Button>
      </form>
      <p className="texto-centro">¿No tienes cuenta? <Link to="/registro">Regístrate</Link></p>
    </AuthLayout>
  );
}
