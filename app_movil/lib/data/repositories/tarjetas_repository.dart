import '../../core/network/api_client.dart';
import '../../core/utils/json.dart';
import '../models/tarjeta.dart';

/// Tarjetas de débito guardadas (mismos endpoints que la web: `/api/tarjetas`).
class TarjetasRepository {
  TarjetasRepository(this._api);

  final ApiClient _api;

  Future<List<TarjetaGuardada>> listar() async {
    final r = await _api.get('/api/tarjetas');
    return asMapList(r.data).map(TarjetaGuardada.fromJson).toList();
  }

  /// [tarjeta] en el formato de `TarjetaUtils.paraApi`.
  Future<TarjetaGuardada> guardar(Map<String, dynamic> tarjeta) async {
    final r = await _api.post('/api/tarjetas', body: tarjeta);
    return TarjetaGuardada.fromJson(asMap(r.data));
  }

  Future<void> eliminar(String id) => _api.delete('/api/tarjetas/${Uri.encodeComponent(id)}');
}
