import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../../core/utils/tarjeta.dart';
import '../../data/models/producto.dart';
import '../../data/models/tarjeta.dart';
import '../../data/models/validacion.dart';
import '../../data/repositories/pagos_repository.dart';
import '../../data/repositories/tarjetas_repository.dart';
import '../../state/auth_controller.dart';
import '../../state/historial_controller.dart';
import '../widgets/app_text_field.dart';
import '../widgets/common.dart';
import '../widgets/formulario_tarjeta.dart';
import '../widgets/proveedor_logo.dart';
import '../widgets/state_views.dart';
import 'resultado_pago_screen.dart';

/// Paso 2: resumen de la operación, tarjeta de débito y confirmación explícita antes de cobrar (igual que la web).
class ConfirmarPagoScreen extends StatefulWidget {
  const ConfirmarPagoScreen({
    super.key,
    required this.producto,
    required this.referencia,
    required this.validacion,
    required this.monto,
  });

  final Producto producto;
  final String referencia;
  final ValidacionReferencia validacion;
  final double monto;

  @override
  State<ConfirmarPagoScreen> createState() => _ConfirmarPagoScreenState();
}

class _ConfirmarPagoScreenState extends State<ConfirmarPagoScreen> {
  late final String _idempotencyKey;
  bool _procesando = false;
  bool _resultadoIncierto = false;
  String? _error;

  static const _nueva = 'NUEVA';
  final _formKey = GlobalKey<FormState>();
  final _tarjetaNueva = TarjetaControllers();
  final _cvvGuardada = TextEditingController();
  List<TarjetaGuardada>? _tarjetas;
  String? _seleccion;
  bool _guardarTarjeta = true;

  List<TarjetaGuardada> get _usables => (_tarjetas ?? const <TarjetaGuardada>[]).where((t) => !t.vencida).toList();
  bool get _usarNueva => _seleccion == _nueva || !_usables.any((t) => t.id == _seleccion);
  String get _descripcionMedio => _usarNueva
      ? 'tu tarjeta de débito •••• ${_ultimos4(_tarjetaNueva.numero.text)}'
      : 'tu tarjeta ${_usables.firstWhere((t) => t.id == _seleccion).descripcion}';

  static String _ultimos4(String numero) {
    final d = TarjetaUtils.soloDigitos(numero);
    return d.length >= 4 ? d.substring(d.length - 4) : d;
  }

  @override
  void initState() {
    super.initState();
    // Una llave por operación: si hay que reintentar, se reutiliza y el backend no cobra dos veces.
    _idempotencyKey = context.read<PagosRepository>().nuevaIdempotencyKey();
    _cargarTarjetas();
  }

  @override
  void dispose() {
    _tarjetaNueva.dispose();
    _cvvGuardada.dispose();
    super.dispose();
  }

  Future<void> _cargarTarjetas() async {
    List<TarjetaGuardada> lista;
    try {
      lista = await context.read<TarjetasRepository>().listar();
    } on ApiException {
      lista = const [];
    }
    if (!mounted) return;
    setState(() {
      _tarjetas = lista;
      // Si tiene tarjetas guardadas se propone la más reciente; si no, el formulario de tarjeta nueva.
      _seleccion ??= _usables.isNotEmpty ? _usables.first.id : _nueva;
    });
  }

  void _elegir(String? id) => setState(() {
        _seleccion = id;
        _cvvGuardada.clear();
        _error = null;
      });

  Future<void> _confirmarYPagar() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        icon: const Icon(Icons.payments_outlined),
        title: const Text('¿Confirmas el pago?'),
        content: Text(
          'Se cargarán ${formatMoney(widget.monto)} a $_descripcionMedio, de contado, para pagar '
          '${widget.producto.nombre} (referencia ${widget.validacion.referenciaEnmascarada}).',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Sí, pagar')),
        ],
      ),
    );
    if (ok == true) await _pagar();
  }

  Future<void> _pagar() async {
    final auth = context.read<AuthController>();
    final repo = context.read<PagosRepository>();
    final cuenta = auth.perfil?.numeroCuenta;
    if (cuenta == null || cuenta.isEmpty) {
      setState(() => _error = 'No se encontró tu número de cuenta. Cierra sesión y vuelve a entrar.');
      return;
    }
    setState(() {
      _procesando = true;
      _error = null;
    });
    try {
      final pago = await repo.pagar(
        idempotencyKey: _idempotencyKey,
        numeroCuenta: cuenta,
        producto: widget.producto,
        referencia: widget.referencia,
        validacionId: widget.validacion.validacionId,
        monto: widget.monto,
        tarjetaNueva: _usarNueva ? _tarjetaNueva.paraApi() : null,
        guardarTarjeta: _usarNueva && _guardarTarjeta,
        tarjetaGuardadaId: _usarNueva ? null : _seleccion,
        cvv: _usarNueva ? null : _cvvGuardada.text,
      );
      _tarjetaNueva.limpiar();
      _cvvGuardada.clear();
      if (!mounted) return;
      context.read<HistorialController>().recargar();
      auth.recargarPerfil();
      Navigator.of(context).pushReplacement(MaterialPageRoute(
        builder: (_) => ResultadoPagoScreen(pago: pago, logoUrl: widget.producto.logoUrl),
      ));
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _resultadoIncierto = e.isNetwork;
        if (e.isNetwork) {
          _error = 'No recibimos respuesta del servidor, así que no sabemos si el pago se procesó. '
              'Reintenta: se usa la misma operación y no se cobrará dos veces.';
        } else if (e.statusCode == 404 && !_usarNueva) {
          _error = '${e.message} Elige otra tarjeta o agrega una nueva.';
          _seleccion = null;
          _cargarTarjetas();
        } else if (e.statusCode == 409) {
          _error = '${e.message} Revisa tu historial antes de intentar de nuevo.';
        } else {
          _error = e.fullMessage;
        }
      });
    } finally {
      if (mounted) setState(() => _procesando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.producto;
    final v = widget.validacion;
    final perfil = context.watch<AuthController>().perfil;
    final cuenta = perfil?.numeroCuenta ?? '';
    final cuentaMask = cuenta.length > 4 ? '•••• ${cuenta.substring(cuenta.length - 4)}' : cuenta;

    return PopScope(
      canPop: !_procesando,
      child: Scaffold(
        appBar: AppBar(title: const Text('Confirmar pago')),
        body: SafeArea(
          child: ListView(
            children: [
              ResponsiveBody(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(child: ProveedorLogo(nombre: p.servicio.isEmpty ? p.nombre : p.servicio, url: p.logoUrl, size: 72, otrosNombres: [p.nombre])),
                    const SizedBox(height: 12),
                    Text('Total a pagar', textAlign: TextAlign.center, style: Theme.of(context).textTheme.bodyMedium),
                    Text(formatMoney(widget.monto),
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.displaySmall?.copyWith(fontWeight: FontWeight.w700)),
                    const SizedBox(height: 16),
                    SectionCard(
                      title: 'Resumen de la operación',
                      children: [
                        InfoRow(label: 'Servicio', value: v.servicio ?? p.servicio),
                        InfoRow(label: 'Producto', value: v.producto ?? p.nombre),
                        InfoRow(label: 'Categoría', value: p.categoria),
                        InfoRow(label: 'Referencia', value: v.referenciaEnmascarada),
                        InfoRow(label: 'Cuenta', value: cuentaMask),
                        const InfoRow(label: 'Forma de pago', value: 'Tarjeta de débito, de contado (un solo cargo)'),
                        const Divider(),
                        InfoRow(label: 'Monto', value: formatMoney(widget.monto), emphasize: true),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _seccionTarjeta(context),
                    const SizedBox(height: 12),
                    if (_error != null) ...[
                      MessageBanner(
                        message: _error!,
                        kind: _resultadoIncierto ? BannerKind.warning : BannerKind.error,
                      ),
                      const SizedBox(height: 12),
                    ],
                    LoadingButton(
                      label: _resultadoIncierto ? 'Reintentar pago' : 'Pagar ${formatMoney(widget.monto)}',
                      icon: Icons.lock_outline,
                      loading: _procesando,
                      onPressed: _resultadoIncierto ? _pagar : _confirmarYPagar,
                    ),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: _procesando ? null : () => Navigator.pop(context),
                      child: const Text('Cancelar'),
                    ),
                    if (_procesando)
                      const Padding(
                        padding: EdgeInsets.only(top: 8),
                        child: Text('Procesando con Gestopago, no cierres la app…', textAlign: TextAlign.center),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _seccionTarjeta(BuildContext context) {
    final bloqueado = _procesando || _resultadoIncierto;
    return Form(
      key: _formKey,
      child: SectionCard(
        title: 'Paga con tu tarjeta de débito',
        children: [
          if (_tarjetas == null)
            const Padding(padding: EdgeInsets.all(12), child: LoadingView(message: 'Buscando tus tarjetas…'))
          else ...[
            // Selección manual (sin RadioGroup) para funcionar igual en Flutter 3.27 y en versiones nuevas
            for (final t in _usables)
              _OpcionTarjeta(
                seleccionada: !_usarNueva && _seleccion == t.id,
                icono: Icons.credit_card,
                titulo: t.descripcion,
                subtitulo: 'Vence ${t.vencimiento}',
                onTap: bloqueado ? null : () => _elegir(t.id),
              ),
            if (_usables.isNotEmpty)
              _OpcionTarjeta(
                seleccionada: _usarNueva,
                icono: Icons.add_card,
                titulo: 'Agregar una tarjeta nueva',
                onTap: bloqueado ? null : () => _elegir(_nueva),
              ),
            if (!_usarNueva)
              AppTextField(
                controller: _cvvGuardada,
                label: 'CVV',
                hint: '123',
                obscure: true,
                enabled: !bloqueado,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(3)],
                validator: (v) => TarjetaUtils.validarCvv(v) == null ? null : 'Escribe los 3 dígitos del reverso',
              )
            else ...[
              if (_usables.isEmpty)
                const Padding(
                  padding: EdgeInsets.only(bottom: 8),
                  child: Text('No tienes tarjetas guardadas. Agrega tu tarjeta de débito para pagar.'),
                ),
              IgnorePointer(ignoring: bloqueado, child: FormularioTarjeta(controllers: _tarjetaNueva)),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                controlAffinity: ListTileControlAffinity.leading,
                value: _guardarTarjeta,
                onChanged: bloqueado ? null : (v) => setState(() => _guardarTarjeta = v ?? false),
                title: const Text('Guardar esta tarjeta para mis próximos pagos'),
              ),
            ],
          ],
        ],
      ),
    );
  }
}

class _OpcionTarjeta extends StatelessWidget {
  const _OpcionTarjeta({
    required this.seleccionada,
    required this.icono,
    required this.titulo,
    this.subtitulo,
    required this.onTap,
  });

  final bool seleccionada;
  final IconData icono;
  final String titulo;
  final String? subtitulo;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      selected: seleccionada,
      inMutuallyExclusiveGroup: true,
      child: ListTile(
        contentPadding: EdgeInsets.zero,
        onTap: onTap,
        leading: Icon(seleccionada ? Icons.radio_button_checked : Icons.radio_button_off,
            color: seleccionada ? scheme.primary : scheme.onSurfaceVariant),
        title: Text(titulo),
        subtitle: subtitulo == null ? null : Text(subtitulo!),
        trailing: Icon(icono),
      ),
    );
  }
}
