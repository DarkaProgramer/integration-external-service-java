import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';

/// Nombre legible del teléfono ("Samsung SM-A515F"), para que el otro dispositivo sepa quién entró.
Future<String> readDeviceName() async {
  String nombre;
  try {
    final info = DeviceInfoPlugin();
    if (Platform.isAndroid) {
      final a = await info.androidInfo;
      final marca = a.manufacturer.isEmpty ? '' : '${a.manufacturer[0].toUpperCase()}${a.manufacturer.substring(1)} ';
      nombre = '$marca${a.model}';
    } else if (Platform.isIOS) {
      final i = await info.iosInfo;
      nombre = i.utsname.machine.isNotEmpty ? 'iPhone (${i.utsname.machine})' : 'iPhone';
    } else {
      nombre = Platform.operatingSystem;
    }
  } catch (_) {
    nombre = 'Móvil';
  }
  return sanitizeDeviceName(nombre);
}

/// El API solo acepta letras, números, espacios y `.,_()/-`, máximo 100 caracteres.
String sanitizeDeviceName(String raw) {
  final limpio = raw.replaceAll(RegExp(r'[^\p{L}\p{N} .,_()/\-]', unicode: true), '').trim();
  final texto = limpio.isEmpty ? 'Móvil' : limpio;
  return texto.length > 100 ? texto.substring(0, 100) : texto;
}
