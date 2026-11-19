import 'package:flutter/foundation.dart';

import '../core/network/api_client.dart';
import '../core/network/api_exception.dart';
import '../data/models/perfil.dart';
import '../data/models/registro.dart';
import '../data/models/sesion.dart';
import '../data/repositories/auth_repository.dart';

enum AuthStatus { checking, offline, unauthenticated, authenticated }

/// Estado de la sesión de toda la app.
class AuthController extends ChangeNotifier {
  AuthController(this._repo, ApiClient api) {
    api.onSessionExpired = () => _expirar('Tu sesión se cerró (venció o se cerró desde otro dispositivo). Vuelve a iniciar sesión.');
    _repo.onTokensRefreshed = (t) => _aplicar(t, notificar: true);
  }

  final AuthRepository _repo;

  AuthStatus status = AuthStatus.checking;
  Perfil? perfil;
  String? sesionId;

  /// Mensaje para la pantalla de login (sesión vencida o cerrada desde otro dispositivo).
  String? avisoLogin;

  /// Otras sesiones abiertas al momento de entrar (se avisa al usuario una vez).
  List<SesionDispositivo> otrasSesionesAlEntrar = const [];

  bool get isAuthenticated => status == AuthStatus.authenticated;

  Future<void> iniciar() async {
    status = AuthStatus.checking;
    notifyListeners();
    try {
      final t = await _repo.restaurarSesion();
      if (t == null) {
        status = AuthStatus.unauthenticated;
      } else {
        _aplicar(t);
        status = AuthStatus.authenticated;
      }
    } on ApiException catch (e) {
      status = e.isNetwork ? AuthStatus.offline : AuthStatus.unauthenticated;
      if (!e.isNetwork) avisoLogin = 'Tu sesión anterior ya no es válida. Inicia sesión de nuevo.';
    }
    notifyListeners();
  }

  Future<void> login(String correo, String password) async {
    final t = await _repo.login(correo, password);
    _entrar(t);
  }

  Future<void> registrar(RegistroData data) async {
    final t = await _repo.registrar(data);
    _entrar(t);
  }

  Future<void> logout() async {
    await _repo.logout();
    _salir(null);
  }

  Future<void> recargarPerfil() async {
    try {
      perfil = await _repo.perfil();
      notifyListeners();
    } on ApiException {
      // El perfil es informativo; si falla se conserva el anterior.
    }
  }

  void limpiarAvisoOtrasSesiones() => otrasSesionesAlEntrar = const [];

  void _entrar(SesionTokens t) {
    _aplicar(t);
    otrasSesionesAlEntrar = t.otrasSesionesActivas;
    avisoLogin = null;
    status = AuthStatus.authenticated;
    notifyListeners();
  }

  void _aplicar(SesionTokens t, {bool notificar = false}) {
    sesionId = t.sesionId;
    if (t.perfil != null) perfil = t.perfil;
    if (notificar) notifyListeners();
  }

  void _expirar(String mensaje) {
    if (status != AuthStatus.authenticated) return;
    _repo.limpiarLocal();
    _salir(mensaje);
  }

  void _salir(String? mensaje) {
    perfil = null;
    sesionId = null;
    otrasSesionesAlEntrar = const [];
    avisoLogin = mensaje;
    status = AuthStatus.unauthenticated;
    notifyListeners();
  }
}
