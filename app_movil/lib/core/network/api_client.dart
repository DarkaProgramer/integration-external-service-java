import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;

import '../config/app_config.dart';
import 'api_exception.dart';

/// Respuesta exitosa del API: `{codigo, mensaje, timestamp, status, datos}`.
class ApiResult {
  ApiResult(this.statusCode, this.message, this.data);

  final int statusCode;
  final String? message;
  final dynamic data;
}

/// Renueva el accessToken (lo implementa el repositorio de sesión). Devuelve el token nuevo o null.
typedef TokenRefresher = Future<String?> Function();

/// Cliente HTTP único de la app: agrega el Bearer, interpreta la envoltura del API,
/// convierte errores en [ApiException] y, ante un 401, intenta renovar la sesión una vez.
class ApiClient {
  ApiClient({http.Client? httpClient, String? baseUrl})
      : _http = httpClient ?? http.Client(),
        _baseUrl = (baseUrl ?? AppConfig.apiBaseUrl).replaceAll(RegExp(r'/+$'), '');

  final http.Client _http;
  final String _baseUrl;

  String? _accessToken;
  TokenRefresher? _refresher;
  Future<String?>? _refreshEnCurso;

  /// Se llama cuando la sesión ya no se puede renovar (refresh vencido o cerrado desde otro dispositivo).
  void Function()? onSessionExpired;

  void setAccessToken(String? token) => _accessToken = token;
  void setRefresher(TokenRefresher refresher) => _refresher = refresher;

  Future<ApiResult> get(String path, {bool auth = true}) =>
      _send('GET', path, auth: auth);

  Future<ApiResult> post(
    String path, {
    Object? body,
    bool auth = true,
    Map<String, String>? headers,
  }) =>
      _send('POST', path, body: body, auth: auth, headers: headers);

  Future<ApiResult> delete(String path, {bool auth = true}) =>
      _send('DELETE', path, auth: auth);

  Future<ApiResult> _send(
    String method,
    String path, {
    Object? body,
    bool auth = true,
    Map<String, String>? headers,
    bool retried = false,
  }) async {
    final response = await _raw(method, path, body: body, auth: auth, headers: headers);

    if (response.statusCode == 401 && auth && !retried && _refresher != null) {
      final nuevo = await _refreshOnce();
      if (nuevo != null) {
        return _send(method, path, body: body, auth: auth, headers: headers, retried: true);
      }
      onSessionExpired?.call();
    }
    return _parse(response);
  }

  /// Si varias peticiones reciben 401 a la vez, todas esperan la misma renovación.
  Future<String?> _refreshOnce() {
    return _refreshEnCurso ??= _refresher!().whenComplete(() => _refreshEnCurso = null);
  }

  Future<http.Response> _raw(
    String method,
    String path, {
    Object? body,
    required bool auth,
    Map<String, String>? headers,
  }) async {
    final request = http.Request(method, Uri.parse('$_baseUrl$path'));
    request.headers['Accept'] = 'application/json';
    if (body != null) {
      request.headers['Content-Type'] = 'application/json; charset=utf-8';
      request.body = jsonEncode(body);
    }
    if (auth && _accessToken != null) {
      request.headers['Authorization'] = 'Bearer $_accessToken';
    }
    if (headers != null) request.headers.addAll(headers);

    try {
      final streamed = await _http.send(request).timeout(AppConfig.requestTimeout);
      return await http.Response.fromStream(streamed).timeout(AppConfig.requestTimeout);
    } on TimeoutException {
      throw ApiException.network('El servidor tardó demasiado en responder. Inténtalo de nuevo.');
    } on SocketException {
      throw ApiException.network();
    } on http.ClientException {
      throw ApiException.network();
    } on HandshakeException {
      throw ApiException.network('No se pudo establecer una conexión segura con el servidor.');
    }
  }

  ApiResult _parse(http.Response response) {
    Map<String, dynamic>? json;
    if (response.bodyBytes.isNotEmpty) {
      try {
        final decoded = jsonDecode(utf8.decode(response.bodyBytes));
        if (decoded is Map<String, dynamic>) json = decoded;
      } on FormatException {
        json = null;
      }
    }

    final code = response.statusCode;
    if (code >= 200 && code < 300) {
      return ApiResult(code, json?['mensaje']?.toString(), json?['datos']);
    }

    final detalles = <String, String>{};
    final rawDetalles = json?['detalles'];
    if (rawDetalles is Map) {
      rawDetalles.forEach((k, v) => detalles['$k'] = '$v');
    }
    throw ApiException(
      statusCode: code,
      message: json?['mensaje']?.toString() ?? _defaultMessage(code),
      details: detalles,
      body: json,
    );
  }

  static String _defaultMessage(int code) {
    switch (code) {
      case 400:
        return 'Revisa los datos enviados.';
      case 401:
        return 'Tu sesión terminó. Inicia sesión de nuevo.';
      case 403:
        return 'No tienes permiso para esta operación.';
      case 404:
        return 'No se encontró la información solicitada.';
      case 409:
        return 'La operación ya se había realizado.';
      case 429:
        return 'Demasiados intentos. Espera unos minutos.';
      case 503:
        return 'El servicio de Gestopago no está disponible en este momento.';
      default:
        return code >= 500
            ? 'Ocurrió un error en el servidor (HTTP $code).'
            : 'No se pudo completar la operación (HTTP $code).';
    }
  }
}
