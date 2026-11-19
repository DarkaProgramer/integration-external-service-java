import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/sesion.dart';
import '../../state/alertas_controller.dart';
import '../../state/auth_controller.dart';

/// Muestra un aviso cuando la cuenta inicia sesión en otro dispositivo, con opción de cerrar esa sesión.
class SessionAlertListener extends StatefulWidget {
  const SessionAlertListener({super.key, required this.child});

  final Widget child;

  @override
  State<SessionAlertListener> createState() => _SessionAlertListenerState();
}

class _SessionAlertListenerState extends State<SessionAlertListener> {
  late final AlertasController _alertas;
  bool _mostrando = false;

  @override
  void initState() {
    super.initState();
    _alertas = context.read<AlertasController>();
    _alertas.addListener(_revisar);
    WidgetsBinding.instance.addPostFrameCallback((_) => _avisoAlEntrar());
  }

  @override
  void dispose() {
    _alertas.removeListener(_revisar);
    super.dispose();
  }

  /// Al iniciar sesión, si ya había otros dispositivos abiertos, se informa (ellos reciben la alerta).
  void _avisoAlEntrar() {
    final auth = context.read<AuthController>();
    final otras = auth.otrasSesionesAlEntrar;
    if (otras.isEmpty || !mounted) return;
    auth.limpiarAvisoOtrasSesiones();
    final lista = otras.map((s) => s.descripcion).join(', ');
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      duration: const Duration(seconds: 6),
      content: Text('Tu cuenta también está abierta en: $lista. Se les envió un aviso.'),
    ));
  }

  Future<void> _revisar() async {
    if (_mostrando || _alertas.pendientes.isEmpty || !mounted) return;
    _mostrando = true;
    // Se muestra la más reciente; al responder se marcan todas como leídas.
    final alerta = _alertas.pendientes.last;
    final accion = await showDialog<_Accion>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => _AlertaDialog(alerta: alerta),
    );
    try {
      if (accion == _Accion.cerrarOtra) {
        await _alertas.cerrarSesionDe(alerta);
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(const SnackBar(content: Text('Se cerró la sesión del otro dispositivo.')));
        }
      } else {
        await _alertas.descartar();
      }
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      _mostrando = false;
    }
  }

  @override
  Widget build(BuildContext context) => widget.child;
}

enum _Accion { fuiYo, cerrarOtra }

class _AlertaDialog extends StatelessWidget {
  const _AlertaDialog({required this.alerta});

  final AlertaSesion alerta;

  @override
  Widget build(BuildContext context) {
    final tipo = alerta.tipoDispositivo == 'WEB' ? 'la web' : 'un móvil';
    final nombre = (alerta.nombreDispositivo ?? '').isEmpty ? '' : ' (${alerta.nombreDispositivo})';
    return AlertDialog(
      icon: const Icon(Icons.security, size: 36),
      title: const Text('Nuevo inicio de sesión'),
      content: Text(
        'Tu cuenta inició sesión desde $tipo$nombre'
        '${alerta.fecha != null ? ' el ${formatDateTime(alerta.fecha)}' : ''}.\n\n'
        'Si no fuiste tú, cierra esa sesión y cambia tu contraseña.',
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, _Accion.cerrarOtra),
          child: const Text('No fui yo, cerrar esa sesión'),
        ),
        FilledButton(onPressed: () => Navigator.pop(context, _Accion.fuiYo), child: const Text('Fui yo')),
      ],
    );
  }
}
