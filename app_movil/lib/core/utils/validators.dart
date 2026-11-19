/// Validaciones de formularios. Son las mismas reglas que aplica el backend, para avisar antes de enviar.
class Validators {
  Validators._();

  static final _correo = RegExp(r'^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
  static final _password = RegExp(r'^(?=.*\p{L})(?=.*\d).{8,72}$', unicode: true);
  static final _letras = RegExp(r'^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$');
  static final _curp = RegExp(r'^[A-Z]{4}\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$');
  static final _rfc = RegExp(r'^[A-Z&Ñ]{3,4}\d{6}[A-V0-9]{3}$');
  static final _telefono = RegExp(r'^\d{10}$');
  static final _cp = RegExp(r'^\d{5}$');
  static final _alfanumerico = RegExp(r'^[A-Za-z0-9]+$');
  static final _digitos = RegExp(r'^\d+$');

  static String? required(String? v, [String campo = 'Este campo']) {
    if (v != null && v.trim().isNotEmpty) return null;
    return campo.startsWith('La ') ? '$campo es obligatoria' : '$campo es obligatorio';
  }

  static String? email(String? v) {
    final r = required(v, 'El correo');
    if (r != null) return r;
    if (v!.trim().length > 100) return 'Máximo 100 caracteres';
    return _correo.hasMatch(v.trim()) ? null : 'Escribe un correo válido';
  }

  static String? loginPassword(String? v) => required(v, 'La contraseña');

  static String? newPassword(String? v) {
    final r = required(v, 'La contraseña');
    if (r != null) return r;
    return _password.hasMatch(v!)
        ? null
        : 'De 8 a 72 caracteres, con al menos una letra y un número';
  }

  static String? name(String? v, {bool optional = false, String campo = 'Este campo'}) {
    if (optional && (v == null || v.trim().isEmpty)) return null;
    final r = required(v, campo);
    if (r != null) return r;
    final t = v!.trim();
    if (!optional && t.length < 2) return 'Mínimo 2 caracteres';
    if (t.length > 50) return 'Máximo 50 caracteres';
    return _letras.hasMatch(t) ? null : 'Solo letras y espacios';
  }

  static String? curp(String? v) {
    final r = required(v, 'La CURP');
    if (r != null) return r;
    return _curp.hasMatch(v!.trim().toUpperCase()) ? null : 'CURP inválida (18 caracteres)';
  }

  static String? rfc(String? v) {
    final r = required(v, 'El RFC');
    if (r != null) return r;
    return _rfc.hasMatch(v!.trim().toUpperCase()) ? null : 'RFC inválido';
  }

  static String? phone(String? v, {bool optional = false}) {
    if (optional && (v == null || v.trim().isEmpty)) return null;
    final r = required(v, 'El teléfono');
    if (r != null) return r;
    return _telefono.hasMatch(v!.trim()) ? null : 'Deben ser 10 dígitos';
  }

  static String? postalCode(String? v) {
    final r = required(v, 'El código postal');
    if (r != null) return r;
    return _cp.hasMatch(v!.trim()) ? null : 'Deben ser 5 dígitos';
  }

  static String? maxLength(String? v, int max, {bool optional = false, String campo = 'Este campo'}) {
    if (optional && (v == null || v.trim().isEmpty)) return null;
    final r = required(v, campo);
    if (r != null) return r;
    return v!.trim().length > max ? 'Máximo $max caracteres' : null;
  }

  static String? positiveAmount(String? v) {
    final r = required(v, 'El monto');
    if (r != null) return r;
    final n = double.tryParse(v!.trim().replaceAll(',', ''));
    if (n == null || n <= 0) return 'Escribe un monto mayor a cero';
    if (!RegExp(r'^\d+(\.\d{1,2})?$').hasMatch(v.trim().replaceAll(',', ''))) {
      return 'Máximo 2 decimales';
    }
    return null;
  }

  /// Referencia según las reglas del producto (longitud y si son solo dígitos) y el formato general del API.
  static String? reference(String? v, {required int min, required int max, required bool onlyDigits}) {
    final r = required(v, 'La referencia');
    if (r != null) return r;
    final t = v!.trim();
    if (onlyDigits && !_digitos.hasMatch(t)) return 'La referencia solo admite números';
    if (!_alfanumerico.hasMatch(t)) return 'Solo letras y números, sin espacios ni símbolos';
    final lo = min > 0 ? min : 4;
    final hi = max > 0 ? max : 40;
    if (t.length < lo || t.length > hi) {
      return lo == hi ? 'La referencia debe tener $lo caracteres' : 'Debe tener entre $lo y $hi caracteres';
    }
    return null;
  }
}
