/// Error devuelto por el API (`{status, error, mensaje, ruta, detalles}`) o problema de red.
class ApiException implements Exception {
  ApiException({
    required this.statusCode,
    required this.message,
    this.details = const {},
    this.body,
  });

  ApiException.network([String? message])
      : statusCode = 0,
        message = message ??
            'No hay conexión con el servidor. Revisa tu internet e inténtalo de nuevo.',
        details = const {},
        body = null;

  /// 0 = no hubo respuesta (sin red, tiempo agotado).
  final int statusCode;
  final String message;

  /// Errores por campo que manda el API en `detalles`.
  final Map<String, String> details;

  /// Cuerpo JSON completo, por si la pantalla necesita algo más (p. ej. un pago rechazado trae `datos`).
  final Map<String, dynamic>? body;

  bool get isNetwork => statusCode == 0;
  bool get isUnauthorized => statusCode == 401;

  /// Mensaje listo para mostrar, con los errores por campo si los hay.
  String get fullMessage {
    if (details.isEmpty) return message;
    return '$message\n${details.values.map((v) => '• $v').join('\n')}';
  }

  @override
  String toString() => 'ApiException($statusCode): $message';
}
