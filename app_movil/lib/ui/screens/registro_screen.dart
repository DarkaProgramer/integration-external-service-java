import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/validators.dart';
import '../../data/models/registro.dart';
import '../../data/repositories/ubicaciones_repository.dart';
import '../../state/auth_controller.dart';
import '../widgets/app_text_field.dart';
import '../widgets/selector_con_otro.dart';
import '../widgets/common.dart';
import '../widgets/state_views.dart';

/// Alta de usuario. Pide los mismos datos que el onboarding del backend.
class RegistroScreen extends StatefulWidget {
  const RegistroScreen({super.key});

  @override
  State<RegistroScreen> createState() => _RegistroScreenState();
}

class _RegistroScreenState extends State<RegistroScreen> {
  final _form = GlobalKey<FormState>();
  final _c = <String, TextEditingController>{};
  DateTime? _fechaNacimiento;
  String? _sexo;
  String? _estadoCivil;
  bool _enviando = false;
  String? _error;

  // Catálogo de ubicaciones (mismo que la web): países, y estados/municipios de México
  final _ubicaciones = UbicacionesRepository();
  List<String> _paises = const [];
  List<EstadoMx>? _estadosMx;

  bool get _esMexico => UbicacionesRepository.esMexico(c('pais').text);
  List<String> get _opcionesEstado =>
      _esMexico ? (_estadosMx ?? const <EstadoMx>[]).map((e) => e.nombre).toList() : const [];
  List<String> get _opcionesMunicipio {
    if (!_esMexico || _estadosMx == null) return const [];
    for (final e in _estadosMx!) {
      if (e.nombre == c('estado').text) return e.municipios;
    }
    return const [];
  }

  Future<void> _cargarUbicaciones() async {
    try {
      final p = await _ubicaciones.paises();
      final e = await _ubicaciones.estadosDeMexico();
      if (mounted) {
        setState(() {
          _paises = p;
          _estadosMx = e;
        });
      }
    } catch (_) {
      // Sin catálogo se escriben a mano (los selectores muestran campos de texto)
      if (mounted) setState(() => _estadosMx = const []);
    }
  }

  static const _estadosCiviles = ['Soltero', 'Casado', 'Divorciado', 'Viudo', 'Unión libre'];

  TextEditingController c(String k) => _c.putIfAbsent(k, TextEditingController.new);

  @override
  void initState() {
    super.initState();
    c('nacionalidad').text = 'Mexicana';
    c('pais').text = 'México';
    _cargarUbicaciones();
  }

  @override
  void dispose() {
    for (final ctrl in _c.values) {
      ctrl.dispose();
    }
    super.dispose();
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final f = await showDatePicker(
      context: context,
      initialDate: _fechaNacimiento ?? DateTime(hoy.year - 25),
      firstDate: DateTime(1900),
      lastDate: DateTime(hoy.year - 18, hoy.month, hoy.day),
      helpText: 'Fecha de nacimiento',
    );
    if (f != null) {
      setState(() {
        _fechaNacimiento = f;
        c('fecha').text = formatDate(f);
      });
    }
  }

  Future<void> _registrar() async {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate()) {
      setState(() => _error = 'Revisa los campos marcados en rojo.');
      return;
    }
    if (c('password').text != c('password2').text) {
      setState(() => _error = 'Las contraseñas no coinciden.');
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    final data = RegistroData(
      nombre: c('nombre').text,
      segundoNombre: c('segundoNombre').text,
      apellidoPaterno: c('apellidoPaterno').text,
      apellidoMaterno: c('apellidoMaterno').text,
      fechaNacimiento: _fechaNacimiento!,
      curp: c('curp').text,
      rfc: c('rfc').text,
      sexo: _sexo!,
      nacionalidad: c('nacionalidad').text,
      estadoCivil: _estadoCivil!,
      correo: c('correo').text,
      telefonoMovil: c('telefonoMovil').text,
      telefonoAlternativo: c('telefonoAlternativo').text,
      calle: c('calle').text,
      numeroExterior: c('numeroExterior').text,
      numeroInterior: c('numeroInterior').text,
      colonia: c('colonia').text,
      municipio: c('municipio').text,
      estado: c('estado').text,
      codigoPostal: c('codigoPostal').text,
      pais: c('pais').text,
      ocupacion: c('ocupacion').text,
      empresa: c('empresa').text,
      ingresoMensual: double.parse(c('ingresoMensual').text.replaceAll(',', '')),
      password: c('password').text,
    );
    try {
      await context.read<AuthController>().registrar(data);
      if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.fullMessage);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  Widget _titulo(String t) => Padding(
        padding: const EdgeInsets.only(top: 12, bottom: 12),
        child: Text(t, style: Theme.of(context).textTheme.titleMedium),
      );

  /// Dos campos lado a lado en pantallas anchas, uno debajo del otro en teléfonos.
  Widget _par(Widget a, Widget b) => LayoutBuilder(
        builder: (context, box) => box.maxWidth >= 520
            ? Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(child: a),
                const SizedBox(width: 12),
                Expanded(child: b),
              ])
            : Column(children: [a, b]),
      );

  @override
  Widget build(BuildContext context) {
    final mayus = [UpperCaseFormatter()];
    final digitos = [FilteringTextInputFormatter.digitsOnly];
    return Scaffold(
      appBar: AppBar(title: const Text('Crear cuenta')),
      body: SafeArea(
        child: Form(
          key: _form,
          child: ListView(
            children: [
              ResponsiveBody(
                maxWidth: 720,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _titulo('Datos personales'),
                    _par(
                      AppTextField(
                          controller: c('nombre'),
                          label: 'Nombre *',
                          textCapitalization: TextCapitalization.words,
                          validator: (v) => Validators.name(v, campo: 'El nombre')),
                      AppTextField(
                          controller: c('segundoNombre'),
                          label: 'Segundo nombre',
                          textCapitalization: TextCapitalization.words,
                          validator: (v) => Validators.name(v, optional: true)),
                    ),
                    _par(
                      AppTextField(
                          controller: c('apellidoPaterno'),
                          label: 'Apellido paterno *',
                          textCapitalization: TextCapitalization.words,
                          validator: (v) => Validators.name(v, campo: 'El apellido paterno')),
                      AppTextField(
                          controller: c('apellidoMaterno'),
                          label: 'Apellido materno *',
                          textCapitalization: TextCapitalization.words,
                          validator: (v) => Validators.name(v, campo: 'El apellido materno')),
                    ),
                    _par(
                      AppTextField(
                        controller: c('fecha'),
                        label: 'Fecha de nacimiento *',
                        prefixIcon: Icons.cake_outlined,
                        readOnly: true,
                        onTap: _elegirFecha,
                        validator: (_) => _fechaNacimiento == null ? 'Elige tu fecha de nacimiento' : null,
                      ),
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: DropdownButtonFormField<String>(
                          value: _sexo,
                          decoration: const InputDecoration(labelText: 'Sexo *'),
                          items: const [
                            DropdownMenuItem(value: 'F', child: Text('Femenino')),
                            DropdownMenuItem(value: 'M', child: Text('Masculino')),
                          ],
                          onChanged: (v) => setState(() => _sexo = v),
                          validator: (v) => v == null ? 'Elige una opción' : null,
                        ),
                      ),
                    ),
                    _par(
                      AppTextField(
                          controller: c('curp'),
                          label: 'CURP *',
                          maxLength: 18,
                          inputFormatters: mayus,
                          textCapitalization: TextCapitalization.characters,
                          validator: Validators.curp),
                      AppTextField(
                          controller: c('rfc'),
                          label: 'RFC *',
                          maxLength: 13,
                          inputFormatters: mayus,
                          textCapitalization: TextCapitalization.characters,
                          validator: Validators.rfc),
                    ),
                    _par(
                      AppTextField(
                          controller: c('nacionalidad'),
                          label: 'Nacionalidad *',
                          validator: (v) => Validators.maxLength(v, 50, campo: 'La nacionalidad')),
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: DropdownButtonFormField<String>(
                          value: _estadoCivil,
                          decoration: const InputDecoration(labelText: 'Estado civil *'),
                          items: _estadosCiviles.map((e) => DropdownMenuItem(value: e, child: Text(e))).toList(),
                          onChanged: (v) => setState(() => _estadoCivil = v),
                          validator: (v) => v == null ? 'Elige una opción' : null,
                        ),
                      ),
                    ),
                    _titulo('Contacto'),
                    AppTextField(
                        controller: c('correo'),
                        label: 'Correo electrónico *',
                        prefixIcon: Icons.mail_outline,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.email],
                        maxLength: 100,
                        validator: Validators.email),
                    _par(
                      AppTextField(
                          controller: c('telefonoMovil'),
                          label: 'Teléfono móvil *',
                          keyboardType: TextInputType.phone,
                          maxLength: 10,
                          inputFormatters: digitos,
                          validator: (v) => Validators.phone(v)),
                      AppTextField(
                          controller: c('telefonoAlternativo'),
                          label: 'Teléfono alternativo',
                          keyboardType: TextInputType.phone,
                          maxLength: 10,
                          inputFormatters: digitos,
                          validator: (v) => Validators.phone(v, optional: true)),
                    ),
                    _titulo('Domicilio'),
                    AppTextField(
                        controller: c('calle'),
                        label: 'Calle *',
                        validator: (v) => Validators.maxLength(v, 100, campo: 'La calle')),
                    _par(
                      AppTextField(
                          controller: c('numeroExterior'),
                          label: 'Número exterior *',
                          validator: (v) => Validators.maxLength(v, 20, campo: 'El número exterior')),
                      AppTextField(
                          controller: c('numeroInterior'),
                          label: 'Número interior',
                          validator: (v) => Validators.maxLength(v, 20, optional: true)),
                    ),
                    _par(
                      AppTextField(
                          controller: c('colonia'),
                          label: 'Colonia *',
                          validator: (v) => Validators.maxLength(v, 100, campo: 'La colonia')),
                      AppTextField(
                          controller: c('codigoPostal'),
                          label: 'Código postal *',
                          keyboardType: TextInputType.number,
                          maxLength: 5,
                          inputFormatters: digitos,
                          validator: Validators.postalCode),
                    ),
                    // Al cambiar de país se limpian estado y municipio; al cambiar de estado, el municipio (igual que la web)
                    SelectorConOtro(
                      controller: c('pais'),
                      label: 'País *',
                      opciones: _paises,
                      hint: 'Escribe el país',
                      validator: (v) => Validators.maxLength(v, 50, campo: 'El país'),
                      onChanged: (_) => setState(() {
                        c('estado').clear();
                        c('municipio').clear();
                      }),
                    ),
                    _par(
                      SelectorConOtro(
                        key: ValueKey('estado-$_esMexico-${_opcionesEstado.length}'),
                        controller: c('estado'),
                        label: 'Estado *',
                        opciones: _opcionesEstado,
                        cargando: _esMexico && _estadosMx == null,
                        hint: 'Escribe el estado o provincia',
                        validator: (v) => Validators.maxLength(v, 100, campo: 'El estado'),
                        onChanged: (_) => setState(() => c('municipio').clear()),
                      ),
                      SelectorConOtro(
                        key: ValueKey('municipio-${c('estado').text}-${_opcionesMunicipio.length}'),
                        controller: c('municipio'),
                        label: 'Municipio o alcaldía *',
                        opciones: _opcionesMunicipio,
                        cargando: _esMexico && _estadosMx == null,
                        enabled: !(_esMexico && c('estado').text.isEmpty && (_estadosMx?.isNotEmpty ?? false)),
                        hint: _esMexico && c('estado').text.isEmpty ? 'Primero elige el estado' : 'Escribe el municipio o ciudad',
                        validator: (v) => Validators.maxLength(v, 100, campo: 'El municipio'),
                      ),
                    ),
                    _titulo('Información laboral'),
                    _par(
                      AppTextField(
                          controller: c('ocupacion'),
                          label: 'Ocupación *',
                          validator: (v) => Validators.maxLength(v, 100, campo: 'La ocupación')),
                      AppTextField(
                          controller: c('empresa'),
                          label: 'Empresa *',
                          validator: (v) => Validators.maxLength(v, 100, campo: 'La empresa')),
                    ),
                    AppTextField(
                      controller: c('ingresoMensual'),
                      label: 'Ingreso mensual *',
                      prefixIcon: Icons.attach_money,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      validator: Validators.positiveAmount,
                    ),
                    _titulo('Contraseña'),
                    PasswordField(
                      controller: c('password'),
                      helper: 'De 8 a 72 caracteres, con al menos una letra y un número',
                      validator: Validators.newPassword,
                      textInputAction: TextInputAction.next,
                      autofillHints: const [AutofillHints.newPassword],
                    ),
                    PasswordField(
                      controller: c('password2'),
                      label: 'Confirmar contraseña',
                      validator: (v) => v != c('password').text ? 'Las contraseñas no coinciden' : null,
                      autofillHints: const [AutofillHints.newPassword],
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 4),
                      MessageBanner(message: _error!),
                    ],
                    const SizedBox(height: 16),
                    LoadingButton(label: 'Crear cuenta', loading: _enviando, onPressed: _registrar),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Convierte a mayúsculas mientras se escribe (CURP y RFC).
class UpperCaseFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) =>
      newValue.copyWith(text: newValue.text.toUpperCase());
}
