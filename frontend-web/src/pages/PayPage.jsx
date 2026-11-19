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
import { tarjetaParaApi, validarTarjeta } from '../utils/tarjeta';
import { tarjetasApi } from '../api/tarjetasApi';
import FormularioTarjeta, { TARJETA_VACIA, descripcionTarjeta } from '../components/FormularioTarjeta';

const VIGENCIA_VALIDACION_MS = 10 * 60 * 1000;
const PASOS = ['Referencia', 'Pago con tarjeta', 'Comprobante'];
const NUEVA = 'nueva';

function mensajeValidacion(error) {
  if (error?.status === 503) {
    return 'No se pudo validar la referencia con el proveedor (Gestopago) en este momento. Por seguridad no se puede pagar sin validarla; intenta más tarde.';
  }
  return error?.message;
}

export default function PayPage() {
  const { identificador } = useParams();
  const navigate = useNavigate();
  const { perfil } = useAuth();
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
  // Datos de la tarjeta: solo en memoria durante el pago (no se guardan en el navegador ni en el servidor)
  const [tarjeta, setTarjeta] = useState(TARJETA_VACIA);
  const llaveIdempotencia = useRef(null);
  // Tarjetas guardadas (solo marca, terminación y vencimiento) y la que se eligió para este pago
  const { datos: tarjetasGuardadas, cargando: cargandoTarjetas, recargar: recargarTarjetas } = useAsync(() => tarjetasApi.listar().catch(() => []), []);
  const [seleccion, setSeleccion] = useState(null); // token de una tarjeta guardada o NUEVA
  const [cvvGuardada, setCvvGuardada] = useState('');
  const [guardarTarjeta, setGuardarTarjeta] = useState(true);
  const usables = (tarjetasGuardadas ?? []).filter((t) => !t.vencida);

  // Si tiene tarjetas guardadas se propone la más reciente; si no, el formulario de tarjeta nueva.
  useEffect(() => {
    if (seleccion == null && tarjetasGuardadas) setSeleccion(usables[0]?.id ?? NUEVA);
  }, [tarjetasGuardadas]); // eslint-disable-line react-hooks/exhaustive-deps

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

  // Pago de contado: si el proveedor informa un adeudo se paga completo, en un solo cargo a la tarjeta de débito.
  const adeudo = validacion?.montoAdeudo ? Number(validacion.montoAdeudo) : null;
  const montoFinal = adeudo ?? (producto.montoVariable ? Number(monto) : Number(validacion?.precio ?? producto.precio));
  const usarNueva = seleccion === NUEVA || !usables.some((t) => t.id === seleccion);
  const elegir = (id) => {
    setSeleccion(id);
    setCvvGuardada('');
    setErrores({});
    setErrorPago(null);
  };
  const cambiarTarjeta = (campo, valor) => {
    setTarjeta((t) => ({ ...t, [campo]: valor }));
    if (errores[campo]) setErrores((er) => ({ ...er, [campo]: undefined }));
  };
  const vencida = validacion && restante <= 0 && paso === 1;

  const validarFormulario = () => {
    const errs = {};
    const errRef = validarReferencia(referencia, producto.referencia);
    if (errRef) errs.referencia = errRef;
    return errs;
  };

  const validarMonto = () => {
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
    const errTarjeta = usarNueva ? validarTarjeta(tarjeta) : (/^\d{3}$/.test(cvvGuardada) ? {} : { cvvGuardada: 'Escribe los 3 dígitos del reverso' });
    setErrores({ monto: errMonto, ...errTarjeta });
    if (errMonto || Object.keys(errTarjeta).length) return;
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
          ...(usarNueva
            ? { tarjeta: tarjetaParaApi(tarjeta), guardarTarjeta }
            : { tarjetaGuardadaId: seleccion, cvv: cvvGuardada }),
        },
        llaveIdempotencia.current,
      );
      setResultado(pago);
      setPaso(2);
      setTarjeta(TARJETA_VACIA);
      setCvvGuardada('');
      if (usarNueva && guardarTarjeta) recargarTarjetas();
      llaveIdempotencia.current = null;
    } catch (err) {
      // Error de red: se conserva la misma llave para que "Reintentar" no cobre dos veces.
      setErrorPago(err);
      if (err?.status === 404 && !usarNueva) {
        setSeleccion(null);
        recargarTarjetas();
      }
    } finally {
      setPagando(false);
    }
  };

  const consultarEstado = async () => {
    setConsultando(true);
    try {
      setResultado(await pagosApi.consultar(resultado.folio));
    } catch (err) {
      setErrorPago(err);
    } finally {
      setConsultando(false);
    }
  };

  const reiniciar = () => {
    setPaso(0);
    setValidacion(null);
    setTarjeta(TARJETA_VACIA);
    setCvvGuardada('');
    setSeleccion(null);
    recargarTarjetas();
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
          <h2>Paga con tu tarjeta de débito</h2>
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
              ['Forma de pago', 'Tarjeta de débito, de contado (un solo cargo, sin meses ni crédito)'],
              ['Comisión', formatoMoneda(0)],
            ]}
          />
          <div className="total"><span>Total a pagar</span><strong>{montoFinal > 0 ? formatoMoneda(montoFinal) : '—'}</strong></div>
          <h3>Tarjeta de débito</h3>
          {cargandoTarjetas && !tarjetasGuardadas ? <Spinner texto="Buscando tus tarjetas…" /> : (
            <>
              {usables.length > 0 && (
                <ul className="lista-tarjetas" role="radiogroup" aria-label="Tarjetas guardadas">
                  {usables.map((t) => (
                    <li key={t.id} className={seleccion === t.id ? 'seleccionada' : ''}>
                      <label>
                        <input type="radio" name="tarjeta" checked={seleccion === t.id} onChange={() => elegir(t.id)} />
                        <span className="tarjeta-icono" aria-hidden>💳</span>
                        <div>
                          <strong>{descripcionTarjeta(t)}</strong>
                          <div className="texto-suave texto-chico">{t.titular} · Vence {t.vencimiento}</div>
                        </div>
                      </label>
                    </li>
                  ))}
                  <li className={usarNueva ? 'seleccionada' : ''}>
                    <label>
                      <input type="radio" name="tarjeta" checked={usarNueva} onChange={() => elegir(NUEVA)} />
                      <span className="tarjeta-icono" aria-hidden>＋</span>
                      <div><strong>Agregar una tarjeta nueva</strong></div>
                    </label>
                  </li>
                </ul>
              )}
              {!usarNueva && (
                <Field className="cvv-guardada" label="CVV" type="password" value={cvvGuardada} inputMode="numeric" autoComplete="cc-csc"
                  placeholder="123" maxLength={3} autoFocus error={errores.cvvGuardada}
                  onChange={(e) => { setCvvGuardada(e.target.value.replace(/\D/g, '').slice(0, 3)); setErrores((er) => ({ ...er, cvvGuardada: undefined })); }}
                  ayuda="Por seguridad se pide en cada pago" />
              )}
              {usarNueva && (
                <>
                  {usables.length === 0 && <p className="texto-suave texto-chico">No tienes tarjetas guardadas. Agrega tu tarjeta de débito para pagar.</p>}
                  <FormularioTarjeta tarjeta={tarjeta} errores={errores} onChange={cambiarTarjeta} />
                  <label className="check-guardar">
                    <input type="checkbox" checked={guardarTarjeta} onChange={(e) => setGuardarTarjeta(e.target.checked)} />
                    Guardar esta tarjeta para mis próximos pagos
                  </label>
                </>
              )}
              <p className="texto-chico texto-suave">🔒 No guardamos el número completo ni el CVV: solo la marca, los últimos 4 dígitos y el vencimiento.</p>
            </>
          )}
          {vencida && <Alert tipo="aviso" titulo="La validación venció">Vuelve a validar la referencia para continuar.</Alert>}
          {errorPago && (
            <ErrorApi error={errorPago.esDeRed ? { ...errorPago, message: `${errorPago.message} Puedes reintentar: no se cobrará dos veces.` } : errorPago} />
          )}
          <div className="acciones">
            <Button variante="secundario" onClick={reiniciar} disabled={pagando}>Cambiar referencia</Button>
            {vencida
              ? <Button onClick={reiniciar}>Validar de nuevo</Button>
              : <Button onClick={pagar} cargando={pagando} disabled={!validacion.validacionId}>{pagando ? 'Procesando pago…' : errorPago?.esDeRed ? 'Reintentar pago' : `Pagar ${montoFinal > 0 ? formatoMoneda(montoFinal) : ''}`}</Button>}
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
