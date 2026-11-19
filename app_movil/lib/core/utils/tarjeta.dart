import 'package:flutter/services.dart';

/// Validaciones de la tarjeta de débito en la app (las mismas que la web; el backend vuelve a validar todo).
/// El número y el CVV solo viven en memoria mientras se paga: nunca se guardan en el teléfono.
class TarjetaUtils {
  TarjetaUtils._();

  static String soloDigitos(String? v) => (v ?? '').replaceAll(RegExp(r'\D'), '');

  /// 'VISA', 'MASTERCARD', 'AMEX' o null.
  static String? marca(String? numero) {
    final n = soloDigitos(numero);
    if (n.startsWith('4')) return 'VISA';
    final dos = int.tryParse(n.length >= 2 ? n.substring(0, 2) : '') ?? 0;
    final cuatro = int.tryParse(n.length >= 4 ? n.substring(0, 4) : '') ?? 0;
    if ((dos >= 51 && dos <= 55) || (cuatro >= 2221 && cuatro <= 2720)) return 'MASTERCARD';
    if (RegExp(r'^3[47]').hasMatch(n)) return 'AMEX';
    return null;
  }

  static bool luhnValido(String? numero) {
    final n = soloDigitos(numero);
    var suma = 0;
    var doble = false;
    for (var i = n.length - 1; i >= 0; i--) {
      var d = int.parse(n[i]);
      if (doble) {
        d *= 2;
        if (d > 9) d -= 9;
      }
      suma += d;
      doble = !doble;
    }
    return n.isNotEmpty && suma % 10 == 0;
  }

  static String? validarNumero(String? v) {
    final n = soloDigitos(v);
    final m = marca(n);
    if (n.isEmpty) return 'Escribe el número de tu tarjeta';
    if (m == 'AMEX') return 'American Express no se acepta: usa una tarjeta de débito Visa o Mastercard';
    if (m == null) return 'Solo se aceptan tarjetas de débito Visa o Mastercard';
    if (n.length != 16 || !luhnValido(n)) return 'El número de tarjeta no es válido';
    return null;
  }

  static String? validarTitular(String? v) {
    final t = (v ?? '').trim();
    if (t.length < 5) return 'Escribe el nombre como aparece en la tarjeta';
    if (!RegExp(r"^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ .'-]+$").hasMatch(t)) return 'Solo letras';
    return null;
  }

  /// [v] en formato MM/AA.
  static String? validarVencimiento(String? v, [DateTime? hoy]) {
    final ahora = hoy ?? DateTime.now();
    final m = RegExp(r'^(\d{2})/(\d{2})$').firstMatch(v ?? '');
    final mes = m == null ? 0 : int.parse(m.group(1)!);
    if (m == null || mes < 1 || mes > 12) return 'Usa el formato MM/AA';
    final anio = 2000 + int.parse(m.group(2)!);
    if (anio < ahora.year || (anio == ahora.year && mes < ahora.month)) return 'La tarjeta está vencida';
    return null;
  }

  static String? validarCvv(String? v) =>
      RegExp(r'^\d{3}$').hasMatch(v ?? '') ? null : '3 dígitos al reverso';

  /// Cuerpo que espera la API (`TarjetaDto`).
  static Map<String, dynamic> paraApi({
    required String numero,
    required String titular,
    required String vencimiento,
    required String cvv,
  }) {
    final partes = vencimiento.split('/');
    return {
      'numero': soloDigitos(numero),
      'titular': titular.trim(),
      'mesVencimiento': int.parse(partes[0]),
      'anioVencimiento': 2000 + int.parse(partes[1]),
      'cvv': cvv,
    };
  }
}

/// "4000123456759010" → "4000 1234 5675 9010" mientras se escribe.
class NumeroTarjetaFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final d = TarjetaUtils.soloDigitos(newValue.text);
    final recortado = d.length > 16 ? d.substring(0, 16) : d;
    final texto = recortado.replaceAllMapped(RegExp(r'(\d{4})(?=\d)'), (m) => '${m[1]} ');
    return TextEditingValue(text: texto, selection: TextSelection.collapsed(offset: texto.length));
  }
}

/// "1228" → "12/28" mientras se escribe.
class VencimientoFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final d = TarjetaUtils.soloDigitos(newValue.text);
    final r = d.length > 4 ? d.substring(0, 4) : d;
    final texto = r.length > 2 ? '${r.substring(0, 2)}/${r.substring(2)}' : r;
    return TextEditingValue(text: texto, selection: TextSelection.collapsed(offset: texto.length));
  }
}
