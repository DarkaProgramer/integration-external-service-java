import '../../core/utils/json.dart';

/// Tarjeta de débito guardada: el servidor solo conserva token, marca, últimos 4 dígitos y vencimiento.
class TarjetaGuardada {
  const TarjetaGuardada({
    required this.id,
    required this.marca,
    required this.ultimos4,
    required this.vencimiento,
    this.titular,
    this.vencida = false,
  });

  /// Token opaco: se envía como `tarjetaGuardadaId` al pagar.
  final String id;
  final String marca;
  final String ultimos4;

  /// MM/AA
  final String vencimiento;
  final String? titular;
  final bool vencida;

  String get descripcion => '${marca == 'MASTERCARD' ? 'Mastercard' : 'Visa'} •••• $ultimos4';

  factory TarjetaGuardada.fromJson(Map<String, dynamic> j) => TarjetaGuardada(
        id: asString(j['id']) ?? '',
        marca: asString(j['marca']) ?? '',
        ultimos4: asString(j['ultimos4']) ?? '',
        vencimiento: asString(j['vencimiento']) ?? '',
        titular: asString(j['titular']),
        vencida: asBool(j['vencida']),
      );
}
