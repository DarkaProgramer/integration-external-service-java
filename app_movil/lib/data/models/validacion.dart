import '../../core/utils/json.dart';

/// Comprobante de `POST /api/pagos/referencias/validar`. Sin él no se puede pagar.
class ValidacionReferencia {
  const ValidacionReferencia({
    required this.validacionId,
    this.servicio,
    this.producto,
    required this.referenciaEnmascarada,
    this.precio,
    this.montoAdeudo,
    this.expiraEn,
  });

  final String validacionId;
  final String? servicio;
  final String? producto;
  final String referenciaEnmascarada;
  final double? precio;
  final double? montoAdeudo;
  final DateTime? expiraEn;

  factory ValidacionReferencia.fromJson(Map<String, dynamic> j) => ValidacionReferencia(
        validacionId: asString(j['validacionId']) ?? '',
        servicio: asString(j['servicio']),
        producto: asString(j['producto']),
        referenciaEnmascarada: asString(j['referencia']) ?? '',
        precio: asDouble(j['precio']),
        montoAdeudo: asDouble(j['montoAdeudo']),
        expiraEn: asDate(j['expiraEn']),
      );
}
