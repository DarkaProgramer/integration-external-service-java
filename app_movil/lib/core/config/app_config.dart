/// Configuración que llega al compilar con `--dart-define` (nada sensible va en el código).
///
/// ```
/// flutter run --dart-define=API_BASE_URL=https://hasani-api.onrender.com
/// ```
class AppConfig {
  AppConfig._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'https://hasani-api.onrender.com',
  );

  /// Render (plan gratis) tarda hasta ~50 s en despertar, por eso el tiempo de espera es amplio.
  static const Duration requestTimeout = Duration(seconds: 60);

  /// Cada cuánto se consulta si la cuenta inició sesión en otro dispositivo.
  static const Duration alertPollInterval = Duration(seconds: 20);

  static const int historialPageSize = 20;
}
