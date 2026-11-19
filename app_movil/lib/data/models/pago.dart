import '../../core/utils/json.dart';

/// Resultado para la interfaz. EXITOSO solo cuando Gestopago lo confirmó.
enum ResultadoPago {
  exitoso('EXITOSO', 'Exitoso'),
  rechazado('RECHAZADO', 'Rechazado'),
  pendiente('PENDIENTE', 'Pendiente');

  const ResultadoPago(this.api, this.label);
  final String api;
  final String label;

  /// Cualquier valor desconocido se trata como pendiente: nunca se muestra éxito sin confirmación.
  static ResultadoPago fromApi(String? v) => switch (v) {
        'EXITOSO' => ResultadoPago.exitoso,
        'RECHAZADO' => ResultadoPago.rechazado,
        _ => ResultadoPago.pendiente,
      };
}

/// Comprobante / registro del historial.
class Pago {
  const Pago({
    required this.folio,
    this.estatus,
    required this.resultado,
    this.mensaje,
    this.idServicio,
    this.idProducto,
    this.servicio,
    this.producto,
    this.referencia,
    this.monto,
    this.folioProveedor,
    this.fechaOperacion,
    this.tarjeta,
    this.autorizacion,
  });

  final String folio;
  final String? estatus;
  final ResultadoPago resultado;
  final String? mensaje;
  final String? idServicio;
  final String? idProducto;
  final String? servicio;
  final String? producto;
  final String? referencia;
  final double? monto;
  final String? folioProveedor;
  final DateTime? fechaOperacion;

  /// Tarjeta de débito con la que se pagó (p. ej. "Visa •••• 9010").
  final String? tarjeta;

  /// Número de autorización del banco.
  final String? autorizacion;

  factory Pago.fromJson(Map<String, dynamic> j) => Pago(
        folio: asString(j['folio']) ?? '',
        estatus: asString(j['estatus']),
        resultado: ResultadoPago.fromApi(asString(j['resultado'])),
        mensaje: asString(j['mensaje']),
        idServicio: asString(j['idServicio']),
        idProducto: asString(j['idProducto']),
        servicio: asString(j['servicio']),
        producto: asString(j['producto']),
        referencia: asString(j['referencia']),
        monto: asDouble(j['monto']),
        folioProveedor: asString(j['folioProveedor']),
        fechaOperacion: asDate(j['fechaOperacion']),
        tarjeta: asString(j['tarjeta']),
        autorizacion: asString(j['autorizacion']),
      );
}

class PaginaPagos {
  const PaginaPagos({
    required this.contenido,
    required this.pagina,
    required this.totalPaginas,
    required this.totalElementos,
  });

  final List<Pago> contenido;
  final int pagina;
  final int totalPaginas;
  final int totalElementos;

  bool get hayMas => pagina + 1 < totalPaginas;

  factory PaginaPagos.fromJson(Map<String, dynamic> j) => PaginaPagos(
        contenido: asMapList(j['contenido']).map(Pago.fromJson).toList(),
        pagina: asInt(j['pagina']),
        totalPaginas: asInt(j['totalPaginas']),
        totalElementos: asInt(j['totalElementos']),
      );
}
