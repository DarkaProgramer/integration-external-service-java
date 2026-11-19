import '../../core/utils/json.dart';
import 'perfil.dart';

/// Dispositivo con sesión abierta en la cuenta.
class SesionDispositivo {
  const SesionDispositivo({
    required this.sesionId,
    this.tipoDispositivo,
    this.nombreDispositivo,
    this.inicio,
    this.ultimoUso,
    this.actual = false,
  });

  final String sesionId;
  final String? tipoDispositivo;
  final String? nombreDispositivo;
  final DateTime? inicio;
  final DateTime? ultimoUso;
  final bool actual;

  String get descripcion {
    final tipo = tipoDispositivo == 'WEB' ? 'Web' : (tipoDispositivo == 'MOVIL' ? 'Móvil' : 'Dispositivo');
    final nombre = (nombreDispositivo ?? '').trim();
    return nombre.isEmpty ? tipo : '$tipo · $nombre';
  }

  factory SesionDispositivo.fromJson(Map<String, dynamic> j) => SesionDispositivo(
        sesionId: asString(j['sesionId']) ?? '',
        tipoDispositivo: asString(j['tipoDispositivo']),
        nombreDispositivo: asString(j['nombreDispositivo']),
        inicio: asDate(j['inicio']),
        ultimoUso: asDate(j['ultimoUso']),
        actual: asBool(j['actual']),
      );
}

/// Respuesta de registro, login y refresh.
class SesionTokens {
  const SesionTokens({
    required this.accessToken,
    required this.refreshToken,
    required this.sesionId,
    this.expiraEn = 900,
    this.perfil,
    this.otrasSesionesActivas = const [],
  });

  final String accessToken;
  final String refreshToken;
  final String sesionId;
  final int expiraEn;
  final Perfil? perfil;
  final List<SesionDispositivo> otrasSesionesActivas;

  factory SesionTokens.fromJson(Map<String, dynamic> j) => SesionTokens(
        accessToken: asString(j['accessToken']) ?? '',
        refreshToken: asString(j['refreshToken']) ?? '',
        sesionId: asString(j['sesionId']) ?? '',
        expiraEn: asInt(j['expiraEn'], 900),
        perfil: j['perfil'] == null ? null : Perfil.fromJson(asMap(j['perfil'])),
        otrasSesionesActivas: asMapList(j['otrasSesionesActivas']).map(SesionDispositivo.fromJson).toList(),
      );
}

/// Aviso: la cuenta inició sesión en otro dispositivo.
class AlertaSesion {
  const AlertaSesion({
    required this.id,
    required this.mensaje,
    this.sesionId,
    this.tipoDispositivo,
    this.nombreDispositivo,
    this.fecha,
  });

  final int id;
  final String mensaje;
  final String? sesionId;
  final String? tipoDispositivo;
  final String? nombreDispositivo;
  final DateTime? fecha;

  factory AlertaSesion.fromJson(Map<String, dynamic> j) => AlertaSesion(
        id: asInt(j['id']),
        mensaje: asString(j['mensaje']) ?? 'Tu cuenta inició sesión en otro dispositivo.',
        sesionId: asString(j['sesionId']),
        tipoDispositivo: asString(j['tipoDispositivo']),
        nombreDispositivo: asString(j['nombreDispositivo']),
        fecha: asDate(j['fecha']),
      );
}
