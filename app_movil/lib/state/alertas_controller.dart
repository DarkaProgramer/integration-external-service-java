import 'dart:async';

import 'package:flutter/widgets.dart';

import '../core/config/app_config.dart';
import '../core/network/api_exception.dart';
import '../data/models/sesion.dart';
import '../data/repositories/auth_repository.dart';

/// Consulta cada ~20 s si la cuenta inició sesión en otro dispositivo (solo con la app en primer plano).
class AlertasController extends ChangeNotifier with WidgetsBindingObserver {
  AlertasController(this._repo);

  final AuthRepository _repo;
  Timer? _timer;
  bool _activo = false;
  bool _consultando = false;

  /// Alertas pendientes de mostrar.
  final List<AlertaSesion> pendientes = [];

  void iniciar() {
    if (_activo) return;
    _activo = true;
    WidgetsBinding.instance.addObserver(this);
    _programar();
    consultar();
  }

  void detener() {
    if (!_activo) return;
    _activo = false;
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _timer = null;
    pendientes.clear();
  }

  void _programar() {
    _timer?.cancel();
    _timer = Timer.periodic(AppConfig.alertPollInterval, (_) => consultar());
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_activo) return;
    if (state == AppLifecycleState.resumed) {
      _programar();
      consultar();
    } else if (state == AppLifecycleState.paused) {
      _timer?.cancel();
    }
  }

  Future<void> consultar() async {
    if (_consultando || !_activo) return;
    _consultando = true;
    try {
      final nuevas = await _repo.alertas();
      final conocidas = pendientes.map((a) => a.id).toSet();
      final agregadas = nuevas.where((a) => !conocidas.contains(a.id)).toList();
      if (agregadas.isNotEmpty) {
        pendientes.addAll(agregadas);
        notifyListeners();
      }
    } on ApiException {
      // Sin red o servidor dormido: se reintenta en el siguiente ciclo. Un 401 lo maneja el ApiClient.
    } finally {
      _consultando = false;
    }
  }

  /// "Fui yo": se marcan como leídas.
  Future<void> descartar() async {
    pendientes.clear();
    notifyListeners();
    try {
      await _repo.marcarAlertasLeidas();
    } on ApiException {
      // Si falla, la alerta volverá a aparecer en la siguiente consulta.
    }
  }

  /// "No fui yo": cierra la sesión del otro dispositivo y marca las alertas como leídas.
  Future<void> cerrarSesionDe(AlertaSesion alerta) async {
    if (alerta.sesionId != null) {
      await _repo.cerrarSesion(alerta.sesionId!);
    }
    await descartar();
  }

  @override
  void dispose() {
    detener();
    super.dispose();
  }
}
