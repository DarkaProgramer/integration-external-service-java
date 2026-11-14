import { Link, useSearchParams } from 'react-router-dom';
import { pagosApi } from '../api/pagosApi';
import { useAsync } from '../hooks/useAsync';
import { config } from '../config/env';
import PageHeader from '../components/PageHeader';
import StatusBadge from '../components/StatusBadge';
import Pagination from '../components/Pagination';
import EmptyState from '../components/EmptyState';
import Spinner from '../components/Spinner';
import { ErrorApi } from '../components/Alert';
import { formatoFecha, formatoMoneda } from '../utils/formato';

const FILTROS = [['', 'Todos'], ['EXITOSO', 'Exitosos'], ['PENDIENTE', 'Pendientes'], ['RECHAZADO', 'Rechazados']];

export default function HistoryPage() {
  const [params, setParams] = useSearchParams();
  const pagina = Math.max(0, Number(params.get('pagina')) || 0);
  const resultado = params.get('resultado') || '';
  const { datos, cargando, error, recargar } = useAsync(
    () => pagosApi.historial({ pagina, tamano: config.historialTamano, resultado: resultado || undefined }),
    [pagina, resultado],
  );

  const ir = (cambios) => {
    const nuevo = { pagina: String(pagina), resultado, ...cambios };
    setParams(Object.fromEntries(Object.entries(nuevo).filter(([k, v]) => v && !(k === 'pagina' && v === '0'))));
  };
  const pagos = datos?.contenido ?? [];

  return (
    <>
      <PageHeader titulo="Historial de pagos" subtitulo={datos ? `${datos.totalElementos} pago(s)` : 'Tus pagos, del más reciente al más antiguo.'} />
      <div className="chips" role="tablist" aria-label="Filtrar por resultado">
        {FILTROS.map(([valor, texto]) => (
          <button key={valor} role="tab" aria-selected={resultado === valor} className={`chip ${resultado === valor ? 'chip-activo' : ''}`}
            onClick={() => ir({ resultado: valor, pagina: '0' })}>{texto}</button>
        ))}
      </div>
      {error && <ErrorApi error={error} onReintentar={recargar} />}
      {cargando && !datos && <Spinner texto="Cargando historial…" />}
      {datos && pagos.length === 0 && (
        <EmptyState icono="🧾" titulo="Aún no hay pagos" accion={<Link className="btn btn-primario" to="/">Pagar un servicio</Link>}>
          {resultado ? 'No hay pagos con este estado.' : 'Cuando pagues un servicio aparecerá aquí.'}
        </EmptyState>
      )}
      {pagos.length > 0 && (
        <ul className={`lista-pagos ${cargando ? 'atenuado' : ''}`}>
          {pagos.map((p) => (
            <li key={p.folio}>
              <Link to={`/historial/${encodeURIComponent(p.folio)}`} className="pago-fila">
                <div className="pago-fila-info">
                  <strong>{p.producto || p.servicio}</strong>
                  <span className="texto-suave texto-chico">{p.servicio} · Ref. {p.referencia} · {formatoFecha(p.fechaOperacion)}</span>
                </div>
                <div className="pago-fila-monto">
                  <strong>{formatoMoneda(p.monto)}</strong>
                  <StatusBadge resultado={p.resultado} />
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
      <Pagination pagina={pagina} totalPaginas={datos?.totalPaginas} deshabilitado={cargando} onCambiar={(p) => ir({ pagina: String(p) })} />
    </>
  );
}
