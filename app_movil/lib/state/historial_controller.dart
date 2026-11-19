import 'package:flutter/foundation.dart';

import '../core/config/app_config.dart';
import '../core/network/api_exception.dart';
import '../data/models/pago.dart';
import '../data/repositories/pagos_repository.dart';

/// Historial paginado (más reciente primero) con filtro por resultado.
class HistorialController extends ChangeNotifier {
  HistorialController(this._repo);

  final PagosRepository _repo;

  final List<Pago> pagos = [];
  ResultadoPago? filtro;
  bool cargando = false;
  bool cargandoMas = false;
  ApiException? error;
  int _pagina = 0;
  bool hayMas = false;
  int total = 0;
  bool _iniciado = false;

  Future<void> cargarSiHaceFalta() async {
    if (!_iniciado) await recargar();
  }

  Future<void> recargar() async {
    _iniciado = true;
    cargando = true;
    error = null;
    notifyListeners();
    try {
      final p = await _repo.historial(pagina: 0, tamano: AppConfig.historialPageSize, filtro: filtro);
      pagos
        ..clear()
        ..addAll(p.contenido);
      _pagina = 0;
      hayMas = p.hayMas;
      total = p.totalElementos;
    } on ApiException catch (e) {
      error = e;
    } finally {
      cargando = false;
      notifyListeners();
    }
  }

  Future<void> cargarMas() async {
    if (!hayMas || cargandoMas || cargando) return;
    cargandoMas = true;
    notifyListeners();
    try {
      final p = await _repo.historial(pagina: _pagina + 1, tamano: AppConfig.historialPageSize, filtro: filtro);
      pagos.addAll(p.contenido);
      _pagina = p.pagina;
      hayMas = p.hayMas;
    } on ApiException catch (e) {
      error = e;
    } finally {
      cargandoMas = false;
      notifyListeners();
    }
  }

  Future<void> cambiarFiltro(ResultadoPago? f) async {
    filtro = f;
    await recargar();
  }

  /// Actualiza un pago en la lista después de consultar su detalle (p. ej. un pendiente que ya se resolvió).
  void actualizar(Pago pago) {
    final i = pagos.indexWhere((p) => p.folio == pago.folio);
    if (i >= 0) {
      pagos[i] = pago;
      notifyListeners();
    }
  }

  void limpiar() {
    pagos.clear();
    filtro = null;
    _iniciado = false;
    hayMas = false;
    error = null;
  }
}
