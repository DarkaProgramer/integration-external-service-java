import 'package:uuid/uuid.dart';

import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/utils/json.dart';
import '../models/pago.dart';
import '../models/producto.dart';
import '../models/validacion.dart';

/// Validación de referencias, pago, historial y detalle.
class PagosRepository {
  PagosRepository(this._api);

  final ApiClient _api;
  static const _uuid = Uuid();

  /// Llave única por operación: se reutiliza en los reintentos para que no se cobre dos veces.
  String nuevaIdempotencyKey() => _uuid.v4();

  Future<ValidacionReferencia> validarReferencia(Producto p, String referencia) async {
    final r = await _api.post('/api/pagos/referencias/validar', body: {
      'idServicio': p.idServicio,
      'idProducto': p.idProducto,
      'referencia': referencia.trim(),
    });
    return ValidacionReferencia.fromJson(asMap(r.data));
  }

  /// Se paga con tarjeta de débito: [tarjetaGuardadaId] + [cvv], o [tarjetaNueva].
  /// 201 EXITOSO, 202 PENDIENTE, 200 repetición y 422 con `datos` = RECHAZADO devuelven un [Pago].
  /// Cualquier otro error se lanza como [ApiException].
  Future<Pago> pagar({
    required String idempotencyKey,
    required String numeroCuenta,
    required Producto producto,
    required String referencia,
    required String validacionId,
    required double monto,
    String? tarjetaGuardadaId,
    String? cvv,
    Map<String, dynamic>? tarjetaNueva,
    bool guardarTarjeta = false,
  }) async {
    try {
      final r = await _api.post(
        '/api/pagos/servicios',
        headers: {'Idempotency-Key': idempotencyKey},
        body: {
          'numeroCuenta': numeroCuenta,
          'idServicio': producto.idServicio,
          'idProducto': producto.idProducto,
          'referencia': referencia.trim(),
          'validacionId': validacionId,
          'monto': double.parse(monto.toStringAsFixed(2)),
          // Pago de contado con tarjeta de débito: una guardada (+ CVV) o una nueva
          if (tarjetaNueva != null) ...{'tarjeta': tarjetaNueva, 'guardarTarjeta': guardarTarjeta}
          else ...{'tarjetaGuardadaId': tarjetaGuardadaId, 'cvv': cvv},
        },
      );
      return Pago.fromJson(asMap(r.data));
    } on ApiException catch (e) {
      final datos = e.body?['datos'];
      if (e.statusCode == 422 && datos is Map) {
        return Pago.fromJson(asMap(datos));
      }
      rethrow;
    }
  }

  Future<PaginaPagos> historial({int pagina = 0, int tamano = 20, ResultadoPago? filtro}) async {
    final r = await _api.post('/api/pagos/servicios/historial', body: {
      'pagina': pagina,
      'tamano': tamano,
      if (filtro != null) 'resultado': filtro.api,
    });
    return PaginaPagos.fromJson(asMap(r.data));
  }

  Future<Pago> consultar(String folio) async {
    final r = await _api.post('/api/pagos/servicios/consultar', body: {'folio': folio});
    return Pago.fromJson(asMap(r.data));
  }
}
