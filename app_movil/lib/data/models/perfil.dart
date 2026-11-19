import '../../core/utils/json.dart';

class Perfil {
  const Perfil({
    required this.nombre,
    this.apellidoPaterno,
    this.apellidoMaterno,
    required this.correo,
    this.telefonoMovil,
    required this.numeroCuenta,
    this.saldo,
    this.estatusCuenta,
  });

  final String nombre;
  final String? apellidoPaterno;
  final String? apellidoMaterno;
  final String correo;
  final String? telefonoMovil;
  final String numeroCuenta;
  final double? saldo;
  final String? estatusCuenta;

  String get nombreCompleto =>
      [nombre, apellidoPaterno, apellidoMaterno].whereType<String>().where((s) => s.isNotEmpty).join(' ');

  factory Perfil.fromJson(Map<String, dynamic> j) => Perfil(
        nombre: asString(j['nombre']) ?? '',
        apellidoPaterno: asString(j['apellidoPaterno']),
        apellidoMaterno: asString(j['apellidoMaterno']),
        correo: asString(j['correo']) ?? '',
        telefonoMovil: asString(j['telefonoMovil']),
        numeroCuenta: asString(j['numeroCuenta']) ?? '',
        saldo: asDouble(j['saldo']),
        estatusCuenta: asString(j['estatusCuenta']),
      );
}
