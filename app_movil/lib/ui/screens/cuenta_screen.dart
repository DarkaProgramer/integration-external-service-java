import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/config/app_config.dart';
import '../../core/network/api_exception.dart';
import '../../core/utils/formatters.dart';
import '../../data/models/sesion.dart';
import '../../data/repositories/auth_repository.dart';
import '../../state/auth_controller.dart';
import '../widgets/common.dart';
import '../widgets/mis_tarjetas.dart';
import '../widgets/state_views.dart';

/// Perfil, tarjetas de débito, dispositivos con sesión abierta y cierre de sesión (igual que la web).
class CuentaScreen extends StatefulWidget {
  const CuentaScreen({super.key});

  @override
  State<CuentaScreen> createState() => _CuentaScreenState();
}

class _CuentaScreenState extends State<CuentaScreen> {
  List<SesionDispositivo>? _sesiones;
  bool _cargando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _cargar());
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    final auth = context.read<AuthController>();
    final repo = context.read<AuthRepository>();
    try {
      await auth.recargarPerfil();
      final s = await repo.sesiones();
      if (mounted) setState(() => _sesiones = s);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    } finally {
      if (mounted) setState(() => _cargando = false);
    }
  }

  Future<void> _cerrarOtra(SesionDispositivo s) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar sesión en ese dispositivo'),
        content: Text('Se cerrará la sesión en ${s.descripcion}.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Cerrar sesión')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await context.read<AuthRepository>().cerrarSesion(s.sesionId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Sesión cerrada')));
      _cargar();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _logout() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cerrar sesión'),
        content: const Text('¿Quieres salir de tu cuenta en este dispositivo?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Salir')),
        ],
      ),
    );
    if (ok == true && mounted) await context.read<AuthController>().logout();
  }

  @override
  Widget build(BuildContext context) {
    final perfil = context.watch<AuthController>().perfil;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Mi cuenta')),
      body: RefreshIndicator(
        onRefresh: _cargar,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: [
            ResponsiveBody(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_cargando) const LinearProgressIndicator(),
                  if (perfil != null)
                    SectionCard(
                      children: [
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: CircleAvatar(child: Text(initials(perfil.nombreCompleto))),
                          title: Text(perfil.nombreCompleto, style: theme.textTheme.titleMedium),
                          subtitle: Text(perfil.correo),
                        ),
                        const Divider(),
                        InfoRow(label: 'Número de cuenta', value: perfil.numeroCuenta, selectable: true),
                        if (perfil.estatusCuenta != null) InfoRow(label: 'Estatus', value: perfil.estatusCuenta!),
                      ],
                    ),
                  const SizedBox(height: 8),
                  if (_error != null) ...[
                    MessageBanner(message: _error!),
                    const SizedBox(height: 8),
                  ],
                  const MisTarjetas(),
                  const SizedBox(height: 8),
                  SectionCard(
                    title: 'Dispositivos con sesión abierta',
                    children: [
                      if (_sesiones == null && !_cargando) const Text('—'),
                      for (final s in _sesiones ?? const <SesionDispositivo>[])
                        ListTile(
                          contentPadding: EdgeInsets.zero,
                          leading: Icon(s.tipoDispositivo == 'WEB' ? Icons.computer : Icons.smartphone),
                          title: Text(s.descripcion),
                          subtitle: Text(s.actual
                              ? 'Este dispositivo'
                              : 'Último uso: ${formatDateTime(s.ultimoUso ?? s.inicio)}'),
                          trailing: s.actual
                              ? const Icon(Icons.check_circle, color: Color(0xFF1B873F))
                              : TextButton(onPressed: () => _cerrarOtra(s), child: const Text('Cerrar')),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: _logout,
                    icon: const Icon(Icons.logout),
                    label: const Text('Cerrar sesión'),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(48),
                      foregroundColor: theme.colorScheme.error,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text('Servidor: ${AppConfig.apiBaseUrl}',
                      textAlign: TextAlign.center, style: theme.textTheme.bodySmall),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
