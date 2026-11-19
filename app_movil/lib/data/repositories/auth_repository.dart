import '../../core/network/api_client.dart';
import '../../core/network/api_exception.dart';
import '../../core/storage/token_storage.dart';
import '../../core/utils/device_name.dart';
import '../../core/utils/json.dart';
import '../models/perfil.dart';
import '../models/registro.dart';
import '../models/sesion.dart';

/// Endpoints de `/api/auth`: registro, login, refresh, logout, perfil, sesiones y alertas.
class AuthRepository {
  AuthRepository(this._api, this._storage) {
    _api.setRefresher(_refreshDesdeInterceptor);
  }

  final ApiClient _api;
  final TokenStorage _storage;
  String? _deviceName;

  /// Se avisa cuando el refresh automático entrega tokens nuevos (para guardar el perfil).
  void Function(SesionTokens tokens)? onTokensRefreshed;

  Future<Map<String, dynamic>> _dispositivo() async {
    _deviceName ??= await readDeviceName();
    return {'tipo': 'MOVIL', 'nombre': _deviceName};
  }

  Future<SesionTokens> login(String correo, String password) async {
    final r = await _api.post('/api/auth/login', auth: false, body: {
      'correo': correo.trim(),
      'password': password,
      'dispositivo': await _dispositivo(),
    });
    return _guardar(SesionTokens.fromJson(asMap(r.data)));
  }

  Future<SesionTokens> registrar(RegistroData data) async {
    final r = await _api.post('/api/auth/registro', auth: false, body: data.toJson(await _dispositivo()));
    return _guardar(SesionTokens.fromJson(asMap(r.data)));
  }

  /// Renueva la sesión con el refreshToken guardado. Null si no hay sesión o ya no es válida.
  Future<SesionTokens?> restaurarSesion() async {
    final refresh = await _storage.readRefreshToken();
    if (refresh == null || refresh.isEmpty) return null;
    try {
      final r = await _api.post('/api/auth/refresh', auth: false, body: {'refreshToken': refresh});
      return await _guardar(SesionTokens.fromJson(asMap(r.data)));
    } on ApiException catch (e) {
      // Solo se borra la sesión si el servidor la rechazó; sin red se conserva para intentarlo después.
      if (!e.isNetwork) await _limpiar();
      rethrow;
    }
  }

  Future<String?> _refreshDesdeInterceptor() async {
    try {
      final tokens = await restaurarSesion();
      if (tokens != null) onTokensRefreshed?.call(tokens);
      return tokens?.accessToken;
    } on ApiException {
      return null;
    }
  }

  Future<void> logout() async {
    try {
      await _api.post('/api/auth/logout');
    } on ApiException {
      // Aunque falle la red, la sesión local se cierra.
    } finally {
      await _limpiar();
    }
  }

  /// Borra los tokens locales sin llamar al servidor (la sesión ya se cerró en otro lado).
  Future<void> limpiarLocal() => _limpiar();

  Future<Perfil> perfil() async {
    final r = await _api.get('/api/auth/perfil');
    return Perfil.fromJson(asMap(r.data));
  }

  Future<List<SesionDispositivo>> sesiones() async {
    final r = await _api.get('/api/auth/sesiones');
    return asMapList(r.data).map(SesionDispositivo.fromJson).toList();
  }

  Future<void> cerrarSesion(String sesionId) =>
      _api.post('/api/auth/sesiones/cerrar', body: {'sesionId': sesionId});

  Future<List<AlertaSesion>> alertas() async {
    final r = await _api.get('/api/auth/alertas');
    return asMapList(r.data).map(AlertaSesion.fromJson).toList();
  }

  Future<void> marcarAlertasLeidas() => _api.post('/api/auth/alertas/leidas');

  Future<SesionTokens> _guardar(SesionTokens t) async {
    _api.setAccessToken(t.accessToken);
    await _storage.save(refreshToken: t.refreshToken, sesionId: t.sesionId);
    return t;
  }

  Future<void> _limpiar() async {
    _api.setAccessToken(null);
    await _storage.clear();
  }
}
