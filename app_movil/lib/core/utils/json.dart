/// Ayudas para leer JSON sin romperse si un campo falta o viene con otro tipo.
String? asString(Object? v) => v?.toString();

double? asDouble(Object? v) {
  if (v == null) return null;
  if (v is num) return v.toDouble();
  return double.tryParse(v.toString());
}

int asInt(Object? v, [int fallback = 0]) {
  if (v is int) return v;
  if (v is num) return v.toInt();
  return int.tryParse('$v') ?? fallback;
}

bool asBool(Object? v) => v == true || v == 'true';

DateTime? asDate(Object? v) => v == null ? null : DateTime.tryParse(v.toString());

Map<String, dynamic> asMap(Object? v) =>
    v is Map ? v.map((k, val) => MapEntry(k.toString(), val)) : <String, dynamic>{};

List<Map<String, dynamic>> asMapList(Object? v) =>
    v is List ? v.map(asMap).toList() : <Map<String, dynamic>>[];
