import '../../core/network/api_client.dart';
import '../../core/utils/json.dart';
import '../models/producto.dart';

/// Catálogo en vivo de Gestopago (el backend lo consulta y guarda una copia de 30 min).
class CatalogoRepository {
  CatalogoRepository(this._api);

  final ApiClient _api;

  Future<List<Producto>> productos() async {
    final r = await _api.get('/api/catalogo/productos');
    return asMapList(r.data).map(Producto.fromJson).toList();
  }
}
