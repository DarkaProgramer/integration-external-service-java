import 'package:intl/intl.dart';

final NumberFormat _moneda = NumberFormat.currency(locale: 'es_MX', symbol: r'$');
final DateFormat _fechaHora = DateFormat('dd/MM/yyyy HH:mm');
final DateFormat _fecha = DateFormat('dd/MM/yyyy');

String formatMoney(num? value) => value == null ? '—' : _moneda.format(value);

String formatDateTime(DateTime? value) => value == null ? '—' : _fechaHora.format(value);

String formatDate(DateTime? value) => value == null ? '—' : _fecha.format(value);

/// Iniciales para el logo de respaldo ("Telcel" → "TE", "Agua Potable" → "AP").
String initials(String text) {
  final words = text.trim().split(RegExp(r'\s+')).where((w) => w.isNotEmpty).toList();
  if (words.isEmpty) return '?';
  if (words.length == 1) {
    final w = words.first;
    return (w.length >= 2 ? w.substring(0, 2) : w).toUpperCase();
  }
  return (words[0][0] + words[1][0]).toUpperCase();
}
