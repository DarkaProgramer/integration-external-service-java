import 'package:flutter/foundation.dart';

import '../core/network/api_exception.dart';
import '../data/models/producto.dart';
import '../data/repositories/catalogo_repository.dart';

class CatalogoController extends ChangeNotifier {
  CatalogoController(this._repo);

  final CatalogoRepository _repo;

  bool cargando = false;
  ApiException? error;
  List<Producto> _todos = const [];
  String busqueda = '';
  String? categoria;

  bool get cargado => _todos.isNotEmpty;

  /// idServicio → logo, para mostrar el logotipo del proveedor en el historial.
  Map<String, String> get logosPorServicio => {
        for (final p in _todos)
          if ((p.logoUrl ?? '').isNotEmpty) p.idServicio: p.logoUrl!,
      };

  List<String> get categorias {
    final set = _todos.map((p) => p.categoria).toSet().toList()..sort();
    return set;
  }

  List<Producto> get visibles {
    final q = busqueda.trim().toLowerCase();
    return _todos.where((p) {
      if (categoria != null && p.categoria != categoria) return false;
      if (q.isEmpty) return true;
      return p.nombre.toLowerCase().contains(q) ||
          p.servicio.toLowerCase().contains(q) ||
          (p.descripcion ?? '').toLowerCase().contains(q);
    }).toList();
  }

  Future<void> cargar({bool forzar = false}) async {
    if (cargando || (cargado && !forzar)) return;
    cargando = true;
    error = null;
    notifyListeners();
    try {
      _todos = await _repo.productos();
    } on ApiException catch (e) {
      error = e;
    } finally {
      cargando = false;
      notifyListeners();
    }
  }

  void buscar(String texto) {
    busqueda = texto;
    notifyListeners();
  }

  void filtrarCategoria(String? c) {
    categoria = c;
    notifyListeners();
  }

  void limpiar() {
    _todos = const [];
    busqueda = '';
    categoria = null;
    error = null;
  }
}
