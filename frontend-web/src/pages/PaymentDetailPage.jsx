import { Link, useParams } from 'react-router-dom';
import { pagosApi } from '../api/pagosApi';
import { useAsync } from '../hooks/useAsync';
import PaymentResult from '../components/PaymentResult';
import Spinner from '../components/Spinner';
import Button from '../components/Button';
import { ErrorApi } from '../components/Alert';

export default function PaymentDetailPage() {
  const { folio } = useParams();
  const { datos: pago, cargando, error, recargar } = useAsync(() => pagosApi.consultar(folio), [folio]);

  return (
    <div className="pago">
      <Link to="/historial" className="volver">← Historial</Link>
      {cargando && !pago && <Spinner texto="Consultando pago…" />}
      {error && <ErrorApi error={error.status === 404 ? Object.assign(error, { message: 'No encontramos ese pago en tu cuenta.' }) : error} onReintentar={error.status === 404 ? undefined : recargar} />}
      {pago && (
        <PaymentResult pago={pago}>
          {pago.resultado === 'PENDIENTE' && (
            <div className="acciones"><Button variante="secundario" onClick={recargar} cargando={cargando}>Actualizar estado</Button></div>
          )}
        </PaymentResult>
      )}
    </div>
  );
}
