import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/validators.dart';
import '../../data/models/producto.dart';
import '../../data/models/validacion.dart';
import '../../data/repositories/pagos_repository.dart';
import '../widgets/app_text_field.dart';
import '../widgets/common.dart';
import '../widgets/proveedor_logo.dart';
import '../widgets/state_views.dart';
import 'confirmar_pago_screen.dart';

/// Paso 1 del pago: capturar y validar la referencia. Sin validación correcta no se puede continuar.
class ReferenciaScreen extends StatefulWidget {
  const ReferenciaScreen({super.key, required this.producto});

  final Producto producto;

  @override
  State<ReferenciaScreen> createState() => _ReferenciaScreenState();
}

class _ReferenciaScreenState extends State<ReferenciaScreen> {
  final _form = GlobalKey<FormState>();
  final _montoForm = GlobalKey<FormState>();
  final _referencia = TextEditingController();
  final _monto = TextEditingController();

  bool _validando = false;
  ValidacionReferencia? _validacion;
  String? _referenciaValidada;
  DateTime? _venceLocal;
  String? _error;
  BannerKind _errorKind = BannerKind.error;

  Producto get p => widget.producto;

  /// El monto lo fija el producto (precio) salvo que sea de monto variable.
  bool get _montoFijo => !p.montoVariable && (p.precio != null || _validacion?.precio != null);

  @override
  void dispose() {
    _referencia.dispose();
    _monto.dispose();
    super.dispose();
  }

  void _referenciaCambio(String v) {
    // Si cambia la referencia, la validación anterior ya no aplica.
    if (_validacion != null && v.trim() != _referenciaValidada) {
      setState(() {
        _validacion = null;
        _referenciaValidada = null;
        _error = null;
      });
    }
  }

  Future<void> _validar() async {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate()) return;
    setState(() {
      _validando = true;
      _error = null;
      _validacion = null;
    });
    final ref = _referencia.text.trim();
    try {
      final v = await context.read<PagosRepository>().validarReferencia(p, ref);
      if (!mounted) return;
      setState(() {
        _validacion = v;
        _referenciaValidada = ref;
        _venceLocal = DateTime.now().add(const Duration(minutes: 10));
        if (!_montoFijo && v.montoAdeudo != null) _monto.text = v.montoAdeudo!.toStringAsFixed(2);
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        if (e.statusCode == 422) {
          _errorKind = BannerKind.error;
          _error = 'Referencia rechazada: ${e.fullMessage}';
        } else if (e.statusCode == 503) {
          _errorKind = BannerKind.warning;
          _error = 'No se pudo verificar la referencia con Gestopago en este momento '
              '(${e.message}). No es posible continuar con el pago hasta validarla.';
        } else {
          _errorKind = BannerKind.error;
          _error = e.fullMessage;
        }
      });
    } finally {
      if (mounted) setState(() => _validando = false);
    }
  }

  String? _validarMonto(String? v) {
    final base = Validators.positiveAmount(v);
    if (base != null) return base;
    final n = double.parse(v!.trim().replaceAll(',', ''));
    if (p.montoMinimo != null && n < p.montoMinimo!) return 'El mínimo es ${formatMoney(p.montoMinimo)}';
    if (p.montoMaximo != null && n > p.montoMaximo!) return 'El máximo es ${formatMoney(p.montoMaximo)}';
    return null;
  }

  void _continuar() {
    final v = _validacion;
    if (v == null) return;
    if (_venceLocal != null && DateTime.now().isAfter(_venceLocal!)) {
      setState(() {
        _validacion = null;
        _errorKind = BannerKind.warning;
        _error = 'La validación venció (10 minutos). Valida la referencia otra vez.';
      });
      return;
    }
    double monto;
    if (_montoFijo) {
      monto = (v.precio ?? p.precio)!;
    } else {
      if (!_montoForm.currentState!.validate()) return;
      monto = double.parse(_monto.text.trim().replaceAll(',', ''));
    }
    Navigator.of(context).push(MaterialPageRoute(
      builder: (_) => ConfirmarPagoScreen(
        producto: p,
        referencia: _referenciaValidada!,
        validacion: v,
        monto: monto,
      ),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final v = _validacion;
    return Scaffold(
      appBar: AppBar(title: const Text('Validar referencia')),
      body: SafeArea(
        child: ListView(
          children: [
            ResponsiveBody(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Card(
                    child: ListTile(
                      contentPadding: const EdgeInsets.all(12),
                      leading: ProveedorLogo(nombre: p.servicio.isEmpty ? p.nombre : p.servicio, url: p.logoUrl, otrosNombres: [p.nombre]),
                      title: Text(p.nombre),
                      subtitle: Text([p.servicio, p.categoria].where((s) => s.isNotEmpty).join(' · ')),
                      trailing: p.montoVariable
                          ? null
                          : Text(formatMoney(p.precio), style: theme.textTheme.titleMedium),
                    ),
                  ),
                  if ((p.descripcion ?? '').isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.fromLTRB(4, 8, 4, 0),
                      child: Text(p.descripcion!, style: theme.textTheme.bodyMedium),
                    ),
                  const SizedBox(height: 20),
                  Form(
                    key: _form,
                    child: AppTextField(
                      controller: _referencia,
                      label: 'Referencia',
                      hint: p.refSoloDigitos ? 'Ej. número de teléfono o de servicio' : null,
                      helper: p.referenciaHint,
                      prefixIcon: Icons.tag,
                      keyboardType: p.refSoloDigitos ? TextInputType.number : TextInputType.text,
                      inputFormatters: [
                        if (p.refSoloDigitos) FilteringTextInputFormatter.digitsOnly,
                        FilteringTextInputFormatter.deny(RegExp(r'\s')),
                      ],
                      maxLength: p.refLongitudMaxima > 0 ? p.refLongitudMaxima : 40,
                      textInputAction: TextInputAction.done,
                      onChanged: _referenciaCambio,
                      onSubmitted: (_) => _validar(),
                      validator: (val) => Validators.reference(
                        val,
                        min: p.refLongitudMinima,
                        max: p.refLongitudMaxima,
                        onlyDigits: p.refSoloDigitos,
                      ),
                    ),
                  ),
                  OutlinedButton.icon(
                    onPressed: _validando ? null : _validar,
                    icon: _validando
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.verified_outlined),
                    label: Text(_validando ? 'Verificando con Gestopago…' : 'Validar referencia'),
                    style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(48)),
                  ),
                  const SizedBox(height: 16),
                  if (_error != null) MessageBanner(message: _error!, kind: _errorKind),
                  if (v != null) ...[
                    const MessageBanner(message: 'Referencia válida', kind: BannerKind.success),
                    const SizedBox(height: 8),
                    SectionCard(
                      title: 'Información del servicio',
                      children: [
                        InfoRow(label: 'Servicio', value: v.servicio ?? p.servicio),
                        InfoRow(label: 'Producto', value: v.producto ?? p.nombre),
                        InfoRow(label: 'Referencia', value: v.referenciaEnmascarada),
                        if (v.precio != null) InfoRow(label: 'Precio', value: formatMoney(v.precio)),
                        if (v.montoAdeudo != null) InfoRow(label: 'Monto adeudado', value: formatMoney(v.montoAdeudo)),
                        InfoRow(label: 'Válida hasta', value: formatDateTime(_venceLocal)),
                      ],
                    ),
                    if (!_montoFijo) ...[
                      const SizedBox(height: 12),
                      Form(
                        key: _montoForm,
                        child: AppTextField(
                          controller: _monto,
                          label: 'Monto a pagar',
                          prefixIcon: Icons.attach_money,
                          helper: p.montoMinimo != null && p.montoMaximo != null
                              ? 'Entre ${formatMoney(p.montoMinimo)} y ${formatMoney(p.montoMaximo)}'
                              : null,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          validator: _validarMonto,
                        ),
                      ),
                    ],
                  ],
                  const SizedBox(height: 16),
                  LoadingButton(
                    label: 'Continuar al pago',
                    icon: Icons.arrow_forward,
                    // Bloqueado hasta que la referencia esté validada por el API.
                    onPressed: v == null || _validando ? null : _continuar,
                  ),
                  if (v == null)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text('Primero valida la referencia para poder pagar.',
                          textAlign: TextAlign.center, style: theme.textTheme.bodySmall),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
