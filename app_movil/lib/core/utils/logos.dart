/// Logos de las compañías guardados en `assets/logos` (los mismos que usa la web en /public/logos).
/// Mismas reglas que `frontend-web/src/utils/logos.js`: se busca la marca por nombre del servicio o producto.
class Logos {
  Logos._();

  static final _marcas = <(RegExp, String)>[
    (RegExp(r'telcel', caseSensitive: false), 'telcel'),
    (RegExp(r'at&t|at ?& ?t|\batt\b', caseSensitive: false), 'att'),
    (RegExp(r'movistar', caseSensitive: false), 'movistar'),
    (RegExp(r'\bbait\b', caseSensitive: false), 'bait'),
    (RegExp(r'unefon', caseSensitive: false), 'unefon'),
    (RegExp(r'virgin', caseSensitive: false), 'virgin'),
    (RegExp(r'\bcfe\b|comisi[oó]n federal', caseSensitive: false), 'cfe'),
    (RegExp(r'agua', caseSensitive: false), 'agua'),
    (RegExp(r'telmex', caseSensitive: false), 'telmex'),
    (RegExp(r'izzi', caseSensitive: false), 'izzi'),
    (RegExp(r'totalplay', caseSensitive: false), 'totalplay'),
    (RegExp(r'megacable|mega cable', caseSensitive: false), 'megacable'),
    (RegExp(r'\bsky\b', caseSensitive: false), 'sky'),
    (RegExp(r'\bdish\b', caseSensitive: false), 'dish'),
    (RegExp(r'naturgy|gas natural', caseSensitive: false), 'naturgy'),
    (RegExp(r'netflix', caseSensitive: false), 'netflix'),
    (RegExp(r'spotify', caseSensitive: false), 'spotify'),
    (RegExp(r'xbox', caseSensitive: false), 'xbox'),
  ];

  /// Ruta del asset del logo, o null si la compañía no tiene uno guardado.
  static String? asset(Iterable<String?> nombres) {
    final texto = nombres.whereType<String>().where((n) => n.isNotEmpty).join(' ');
    for (final (re, archivo) in _marcas) {
      if (re.hasMatch(texto)) return 'assets/logos/$archivo.png';
    }
    return null;
  }
}
