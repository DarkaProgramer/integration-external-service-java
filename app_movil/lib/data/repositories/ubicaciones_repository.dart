import 'dart:convert';

import 'package:flutter/services.dart';

/// Estado de México con sus municipios o alcaldías (claves y nombres de INEGI).
class EstadoMx {
  const EstadoMx(this.nombre, this.municipios);

  final String nombre;
  final List<String> municipios;
}

/// Catálogo de ubicaciones del registro: el mismo que usa la web (`frontend-web/src/data`).
/// Va incluido en la app (assets/data), así no depende de una API externa que pueda caerse o pedir llave.
class UbicacionesRepository {
  UbicacionesRepository([AssetBundle? bundle]) : _bundle = bundle ?? rootBundle;

  static const mexico = 'México';
  final AssetBundle _bundle;
  List<String>? _paises;
  List<EstadoMx>? _estados;

  static bool esMexico(String? pais) => (pais ?? '').trim().toLowerCase() == mexico.toLowerCase();

  /// Países en español, con México primero.
  Future<List<String>> paises() async =>
      _paises ??= List<String>.from(jsonDecode(await _bundle.loadString('assets/data/paises.json')) as List);

  Future<List<EstadoMx>> estadosDeMexico() async {
    if (_estados != null) return _estados!;
    final lista = jsonDecode(await _bundle.loadString('assets/data/mexico.json')) as List;
    return _estados = [
      for (final e in lista.cast<Map<String, dynamic>>())
        EstadoMx(e['nombre'] as String, List<String>.from(e['municipios'] as List)),
    ];
  }
}
