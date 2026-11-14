import { request, ApiError } from './http';

export const pagosApi = {
  validarReferencia: ({ idServicio, idProducto, referencia }) =>
    request('/api/pagos/referencias/validar', { method: 'POST', body: { idServicio, idProducto, referencia } }).then((r) => r.datos),

  /**
   * Paga con una Idempotency-Key. 201 = EXITOSO, 422 = RECHAZADO, 202 = PENDIENTE, 200 = repetición.
   * Si falla la red se reintenta con la MISMA llave (el backend no cobra dos veces).
   */
  async pagar(solicitud, idempotencyKey, intentos = 3) {
    let ultimoError;
    for (let i = 0; i < intentos; i++) {
      try {
        const r = await request('/api/pagos/servicios', {
          method: 'POST',
          body: solicitud,
          headers: { 'Idempotency-Key': idempotencyKey },
          aceptar: [202, 422],
        });
        // Un 422 sin datos de pago es un error de validación, no un rechazo del proveedor.
        if (r.status === 422 && !r.datos?.folio) throw new ApiError(422, r.cuerpo?.mensaje || 'No se pudo procesar el pago.', { detalles: r.cuerpo?.detalles });
        return { ...r.datos, mensaje: r.datos?.mensaje || r.cuerpo?.mensaje, repetido: r.headers.get('Idempotent-Replayed') === 'true' };
      } catch (e) {
        ultimoError = e;
        if (!(e instanceof ApiError && e.esDeRed)) throw e;
        await new Promise((res) => setTimeout(res, 1500 * (i + 1)));
      }
    }
    throw ultimoError;
  },

  historial: ({ pagina = 0, tamano = 10, resultado } = {}) =>
    request('/api/pagos/servicios/historial', {
      method: 'POST',
      body: { pagina, tamano, ...(resultado ? { resultado } : {}) },
    }).then((r) => r.datos),

  consultar: (folio) => request('/api/pagos/servicios/consultar', { method: 'POST', body: { folio } }).then((r) => r.datos),
};
