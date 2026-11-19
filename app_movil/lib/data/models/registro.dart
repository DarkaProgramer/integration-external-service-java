/// Datos del formulario de alta. Los nombres coinciden con `RegistroRequestDto` del backend
/// (que rechaza campos desconocidos), por eso solo se envían los campos con valor.
class RegistroData {
  RegistroData({
    required this.nombre,
    this.segundoNombre,
    required this.apellidoPaterno,
    required this.apellidoMaterno,
    required this.fechaNacimiento,
    required this.curp,
    required this.rfc,
    required this.sexo,
    required this.nacionalidad,
    required this.estadoCivil,
    required this.correo,
    required this.telefonoMovil,
    this.telefonoAlternativo,
    required this.calle,
    required this.numeroExterior,
    this.numeroInterior,
    required this.colonia,
    required this.municipio,
    required this.estado,
    required this.codigoPostal,
    required this.pais,
    required this.ocupacion,
    required this.empresa,
    required this.ingresoMensual,
    required this.password,
  });

  final String nombre;
  final String? segundoNombre;
  final String apellidoPaterno;
  final String apellidoMaterno;
  final DateTime fechaNacimiento;
  final String curp;
  final String rfc;
  final String sexo;
  final String nacionalidad;
  final String estadoCivil;
  final String correo;
  final String telefonoMovil;
  final String? telefonoAlternativo;
  final String calle;
  final String numeroExterior;
  final String? numeroInterior;
  final String colonia;
  final String municipio;
  final String estado;
  final String codigoPostal;
  final String pais;
  final String ocupacion;
  final String empresa;
  final double ingresoMensual;
  final String password;

  static String? _opt(String? v) => (v == null || v.trim().isEmpty) ? null : v.trim();

  Map<String, dynamic> toJson(Map<String, dynamic> dispositivo) {
    final f = fechaNacimiento;
    final fecha = '${f.year.toString().padLeft(4, '0')}-${f.month.toString().padLeft(2, '0')}-${f.day.toString().padLeft(2, '0')}';
    final domicilio = <String, dynamic>{
      'calle': calle.trim(),
      'numeroExterior': numeroExterior.trim(),
      if (_opt(numeroInterior) != null) 'numeroInterior': _opt(numeroInterior),
      'colonia': colonia.trim(),
      'municipio': municipio.trim(),
      'estado': estado.trim(),
      'codigoPostal': codigoPostal.trim(),
      'pais': pais.trim(),
    };
    return {
      'nombre': nombre.trim(),
      if (_opt(segundoNombre) != null) 'segundoNombre': _opt(segundoNombre),
      'apellidoPaterno': apellidoPaterno.trim(),
      'apellidoMaterno': apellidoMaterno.trim(),
      'fechaNacimiento': fecha,
      'curp': curp.trim().toUpperCase(),
      'rfc': rfc.trim().toUpperCase(),
      'sexo': sexo,
      'nacionalidad': nacionalidad.trim(),
      'estadoCivil': estadoCivil,
      'correo': correo.trim(),
      'telefonoMovil': telefonoMovil.trim(),
      if (_opt(telefonoAlternativo) != null) 'telefonoAlternativo': _opt(telefonoAlternativo),
      'domicilio': domicilio,
      'ocupacion': ocupacion.trim(),
      'empresa': empresa.trim(),
      'ingresoMensual': ingresoMensual,
      'password': password,
      'dispositivo': dispositivo,
    };
  }
}
