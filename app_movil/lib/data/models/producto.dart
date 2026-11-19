import '../../core/utils/json.dart';

/// Producto del catálogo de Gestopago, tal como lo entrega `GET /api/catalogo/productos`.
class Producto {
  const Producto({
    required this.identificador,
    required this.idServicio,
    required this.idProducto,
    required this.nombre,
    required this.servicio,
    this.descripcion,
    required this.categoria,
    this.logoUrl,
    this.precio,
    this.montoVariable = false,
    this.montoMinimo,
    this.montoMaximo,
    this.refLongitudMinima = 0,
    this.refLongitudMaxima = 0,
    this.refSoloDigitos = false,
  });

  final String identificador;
  final String idServicio;
  final String idProducto;
  final String nombre;
  final String servicio;
  final String? descripcion;
  final String categoria;
  final String? logoUrl;
  final double? precio;
  final bool montoVariable;
  final double? montoMinimo;
  final double? montoMaximo;
  final int refLongitudMinima;
  final int refLongitudMaxima;
  final bool refSoloDigitos;

  String get referenciaHint {
    final tipo = refSoloDigitos ? 'dígitos' : 'caracteres';
    if (refLongitudMinima > 0 && refLongitudMinima == refLongitudMaxima) return '$refLongitudMinima $tipo';
    if (refLongitudMinima > 0 && refLongitudMaxima > 0) return 'De $refLongitudMinima a $refLongitudMaxima $tipo';
    return refSoloDigitos ? 'Solo números' : 'Letras y números';
  }

  factory Producto.fromJson(Map<String, dynamic> j) {
    final ref = asMap(j['referencia']);
    return Producto(
      identificador: asString(j['identificador']) ?? '${j['idServicio']}-${j['idProducto']}',
      idServicio: asString(j['idServicio']) ?? '',
      idProducto: asString(j['idProducto']) ?? '',
      nombre: asString(j['nombre']) ?? 'Producto',
      servicio: asString(j['servicio']) ?? '',
      descripcion: asString(j['descripcion']),
      categoria: asString(j['categoria']) ?? 'Otros',
      logoUrl: asString(j['logoUrl']),
      precio: asDouble(j['precio']),
      montoVariable: asBool(j['montoVariable']),
      montoMinimo: asDouble(j['montoMinimo']),
      montoMaximo: asDouble(j['montoMaximo']),
      refLongitudMinima: asInt(ref['longitudMinima']),
      refLongitudMaxima: asInt(ref['longitudMaxima']),
      refSoloDigitos: asBool(ref['soloDigitos']),
    );
  }
}
