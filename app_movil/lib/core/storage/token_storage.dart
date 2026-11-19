import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Guarda el refreshToken en el almacenamiento seguro del sistema
/// (Android Keystore / iOS Keychain). El accessToken solo vive en memoria.
class TokenStorage {
  TokenStorage([FlutterSecureStorage? storage])
      : _storage = storage ??
            const FlutterSecureStorage(
              aOptions: AndroidOptions(encryptedSharedPreferences: true),
            );

  final FlutterSecureStorage _storage;

  static const _refreshKey = 'refresh_token';
  static const _sesionKey = 'sesion_id';

  Future<String?> readRefreshToken() => _storage.read(key: _refreshKey);

  Future<String?> readSesionId() => _storage.read(key: _sesionKey);

  Future<void> save({required String refreshToken, required String sesionId}) async {
    await _storage.write(key: _refreshKey, value: refreshToken);
    await _storage.write(key: _sesionKey, value: sesionId);
  }

  Future<void> clear() async {
    await _storage.delete(key: _refreshKey);
    await _storage.delete(key: _sesionKey);
  }
}
