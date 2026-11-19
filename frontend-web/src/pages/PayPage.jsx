import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { catalogoApi } from '../api/catalogoApi';
import { pagosApi } from '../api/pagosApi';
import { useAsync } from '../hooks/useAsync';
import { useAuth } from '../auth/AuthContext';
import Button from '../components/Button';
import Field from '../components/Field';
import Spinner from '../components/Spinner';
import Alert, { ErrorApi } from '../components/Alert';
import ProductLogo from '../components/ProductLogo';
import DetailList from '../components/DetailList';
import PaymentResult from '../components/PaymentResult';
import EmptyState from '../components/EmptyState';
import { formatoMoneda, nuevaIdempotencyKey } from '../utils/formato';
import { validarReferencia } from '../utils/validacion';

const VIGENCIA_VALIDACION_MS = 10 * 60 * 1000;
const PASOS = ['Referencia', 'Confirmar', 'Comprobante'];

function mensajeValidacion(error) {
  if (error?.status === 503) {
    return 'No se pudo validar la referencia con el proveedor (Gestopago) en este momento. Por seguridad no se puede pagar sin validarla; intenta más tarde.';
  }
  return error?.message;
}

export default function PayPage() {
  const { identificador } = useParams();
  const navigate = useNavigate();
  const { perfil, recargarPerfil } = useAuth();
  const { datos: producto, cargando, error: errorProducto, recargar } = useAsync(() => catalogoApi.producto(identificador), [identificador]);

  const [paso, setPaso] = useState(0);
  const [referencia, setReferencia] = useState('');
  const [monto, setMonto] = useState('');
  const [errores, setErrores] = useState({});
  const [validacion, setValidacion] = useState(null); // {validacionId, referencia, precio, montoAdeudo, venceEn}
  const [validando, setValidando] = useState(false);
  const [errorValidacion, setErrorValidacion] = useState(null);
  const [pagando, setPagando] = useState(false);
  const [errorPago, setErrorPago] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [consultando, setConsultando] = useState(false);
  const [restante, setRestante] = useState(0);
  const llaveIdempotencia = useRef(null);

  // Cuenta regresiva de la validación (10 min, un solo uso).
  useEffect(() => {
    if (!validacion || paso !== 1) return undefined;
    const tick = () => setRestante(Math.max(0, validacion.venceEn - Date.now()));
    tick();
    const t = setInterval(tick, 1000);
    return () => clearInterval(t);
  }, [validacion, paso]);

  if (cargando) return <Spinner texto="Cargando servicio…" />;
  if (errorProducto) return <ErrorApi error={errorProducto} onReintentar={recargar} />;
  if (!producto) {
    return <EmptyState icono="?" titulo="Servicio no encontrado" accion={<Link className="btn btn-primario" to="/">Ver catálogo</Link>}>Puede que ya no esté disponible.</EmptyState>;
  }

  // Pago de contado: si el proveedor informa un adeudo se paga completo; el cargo es al saldo de la cuenta (sin crédito).
  const adeudo = validacion?.montoAdeudo ? Number(validacion.montoAdeudo) : null;
  const montoFinal = adeudo ?? (producto.montoVariable ? Number(monto) : Number(validacion?.precio ?? producto.precio));
  const saldo = perfil?.saldo == null ? null : Number(perfil.saldo);
  const sinSaldo = saldo != null && montoFinal > 0 && montoFinal > saldo;
  const vencida = validacion && restante <= 0 && paso === 1;

  const validarFormulario = () => {
    const errs = {};
    const errRef = validarReferencia(referencia, producto.referencia);
    if (errRef) errs.referencia = errRef;
    return errs;
  };

  const validarMonto = () => {
    if (sinSaldo) return 'Saldo insuficiente: solo se aceptan pagos de contado con tu saldo disponible';
    if (adeudo != null || !producto.montoVariable) return null;
    const n = Number(monto);
    if (!monto || !(n > 0)) return 'Escribe el monto a pagar';
    if (producto.montoMinimo && n < producto.montoMinimo) return `El monto mínimo es ${formatoMoneda(producto.montoMinimo)}`;
    if (producto.montoMaximo && n > producto.montoMaximo) return `El monto máximo es ${formatoMoneda(producto.montoMaximo)}`;
    if (!/^\d+(\.\d{1,2})?$/.test(String(monto))) return 'Máximo dos decimales';
    return null;
  };

  const validar = async (e) => {
    e.preventDefault();
    const errs = validarFormulario();
    setErrores(errs);
    if (Object.keys(errs).length) return;
    setValidando(true);
    setErrorValidacion(null);
    try {
      const v = await pagosApi.validarReferencia({ idServicio: producto.idServicio, idProducto: producto.idProducto, referencia: referencia.trim() });
      setValidacion({ ...v, venceEn: Date.now() + VIGENCIA_VALIDACION_MS });
      if (producto.montoVariable && v.montoAdeudo) setMonto(String(v.montoAdeudo));
      llaveIdempotencia.current = nuevaIdempotencyKey();
      setPaso(1);
    } catch (err) {
      setErrorValidacion(err);
    } finally {
      setValidando(false);
    }
  };

  const pagar = async () => {
    const errMonto = validarMonto();
    setErrores({ monto: errMonto });
    if (errMonto) return;
    setPagando(true);
    setErrorPago(null);
    try {
      const pago = await pagosApi.pagar(
        {
          numeroCuenta: perfil?.numeroCuenta,
          idServicio: producto.idServicio,
          idProducto: producto.idProducto,
          referencia: referencia.trim(),
          validacionId: validacion.validacionId,
          monto: montoFinal,
        },
        llaveIdempotencia.current,
      );
      setResultado(pago);
      setPaso(2);
      llaveIdempotencia.current = null;
      recargarPerfil().catch(() => {});
    } catch (err) {
      // Error de red: se conserva la misma llave para que "Reintentar" no cobre dos veces.
      setErrorPago(err);
    } finally {
      setPagando(false);
    }
  };

  const consultarEstado = async () => {
    setConsultando(true);
    try {
      setResultado(await pagosApi.consultar(resultado.folio));
      recargarPerfil().catch(() => {});
    } catch (err) {
      setErrorPago(err);
    } finally {
      setConsultando(false);
    }
  };

  const reiniciar = () => {
    setPaso(0);
    setValidacion(null);
    setResultado(null);
    setErrorPago(null);
    setErrorValidacion(null);
  };

  const regla = producto.referencia;
  const ayudaRef = regla
    ? `${regla.soloDigitos ? 'Solo números' : 'Letras y números'}${regla.longitudMinima ? `, ${regla.longitudMinima === regla.longitudMaxima ? regla.longitudMinima : `${regla.longitudMinima} a ${regla.longitudMaxima}`} caracteres` : ''}`
    : 'Número de teléfono, contrato o referencia del recibo';

  return (
    <div className="pago">
      <Link to="/" className="volver">← Catálogo</Link>
      <div className="tarjeta pago-producto">
        <ProductLogo url={producto.logoUrl} nombre={producto.servicio || producto.nombre} tamano={56} />
        <div>
          <h1>{producto.nombre}</h1>
          <p className="texto-suave">{producto.descripcion || producto.servicio}{producto.categoria ? ` · ${producto.categoria}` : ''}</p>
        </div>
      </div>

      <ol className="stepper">
        {PASOS.map((p, i) => <li key={p} className={i === paso ? 'actual' : i < paso ? 'hecho' : ''}><span>{i + 1}</span>{p}</li>)}
      </ol>

      {paso === 0 && (
        <form className="tarjeta formulario" onSubmit={validar} noValidate>
          <h2>Valida tu referencia</h2>
          <p className="texto-suave">Antes de pagar confirmamos con el proveedor que la referencia existe.</p>
          <Field label="Referencia" value={referencia} autoFocus
            inputMode={regla?.soloDigitos ? 'numeric' : 'text'} maxLength={regla?.longitudMaxima || undefined}
            onChange={(e) => { setReferencia(e.target.value); setErrores({}); setErrorValidacion(null); }}
            error={errores.referencia} ayuda={ayudaRef} />
          {errorValidacion && (
            <Alert tipo="error" titulo={errorValidacion.status === 503 ? 'Validación no disponible' : 'No se pudo validar la referencia'}>
              {mensajeValidacion(errorValidacion)}
            </Alert>
          )}
          <Button type="submit" bloque cargando={validando}>{validando ? 'Validando…' : 'Validar referencia'}</Button>
          <p className="texto-chico texto-suave texto-centro">El botón de pagar se habilita solo cuando la referencia está validada.</p>
        </form>
      )}

      {paso === 1 && validacion && (
        <div className="tarjeta">
          <h2>Confirma tu pago</h2>
          <Alert tipo="exito">Referencia validada. {vencida ? '' : `Tienes ${Math.floor(restante / 60000)}:${String(Math.floor(restante / 1000) % 60).padStart(2, '0')} min para pagar.`}</Alert>
          {producto.montoVariable && adeudo == null && (
            <Field label="Monto a pagar (MXN)" type="number" step="0.01" inputMode="decimal" value={monto}
              min={producto.montoMinimo} max={producto.montoMaximo} onChange={(e) => setMonto(e.target.value)} error={errores.monto}
              ayuda={validacion.montoAdeudo ? `Adeudo reportado: ${formatoMoneda(validacion.montoAdeudo)}` : undefined} />
          )}
          <DetailList
            filas={[
              ['Servicio', validacion.servicio || producto.servicio],
              ['Producto', validacion.producto || producto.nombre],
              ['Referencia', validacion.referencia],
              ['Adeudo total', adeudo != null ? formatoMoneda(adeudo) : null],
              ['Forma de pago', 'Contado: cargo único a tu saldo (sin crédito ni pagos parciales)'],
              ['Cuenta de cargo', perfil?.numeroCuenta ? `•••• ${perfil.numeroCuenta.slice(-4)}` : null],
              ['Saldo disponible', formatoMoneda(perfil?.saldo)],
              ['Saldo después del pago', saldo != null && montoFinal > 0 && !sinSaldo ? formatoMoneda(saldo - montoFinal) : null],
            ]}
          />
          <div className="total"><span>Total a pagar</span><strong>{montoFinal > 0 ? formatoMoneda(montoFinal) : '—'}</strong></div>
          {sinSaldo && (
            <Alert tipo="error" titulo="Saldo insuficiente">
              Te faltan {formatoMoneda(montoFinal - saldo)}. Solo se aceptan pagos de contado con el saldo de tu cuenta: no hay crédito ni pagos parciales.
            </Alert>
          )}
          {vencida && <Alert tipo="aviso" titulo="La validación venció">Vuelve a validar la referencia para continuar.</Alert>}
          {errorPago && (
            <ErrorApi error={errorPago.esDeRed ? { ...errorPago, message: `${errorPago.message} Puedes reintentar: no se cobrará dos veces.` } : errorPago} />
          )}
          <div className="acciones">
            <Button variante="secundario" onClick={reiniciar} disabled={pagando}>Cambiar referencia</Button>
            {vencida
              ? <Button onClick={reiniciar}>Validar de nuevo</Button>
              : <Button onClick={pagar} cargando={pagando} disabled={!validacion.validacionId || sinSaldo}>{pagando ? 'Procesando pago…' : errorPago?.esDeRed ? 'Reintentar pago' : `Pagar ${montoFinal > 0 ? formatoMoneda(montoFinal) : ''}`}</Button>}
          </div>
        </div>
      )}

      {paso === 2 && resultado && (
        <PaymentResult pago={resultado}>
          {errorPago && <ErrorApi error={errorPago} />}
          <div className="acciones">
            {resultado.resultado === 'PENDIENTE' && <Button variante="secundario" onClick={consultarEstado} cargando={consultando}>Consultar estado</Button>}
            <Button variante="secundario" onClick={() => navigate('/historial')}>Ver historial</Button>
            <Button onClick={() => navigate('/')}>Pagar otro servicio</Button>
          </div>
        </PaymentResult>
      )}
    </div>
  );
}
