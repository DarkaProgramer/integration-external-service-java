import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../data/models/tarjeta.dart';
import '../../data/repositories/tarjetas_repository.dart';
import 'common.dart';
import 'formulario_tarjeta.dart';
import 'state_views.dart';

/// "Mis tarjetas" de la pantalla de cuenta: listar, agregar y eliminar tarjetas de débito (igual que la web).
class MisTarjetas extends StatefulWidget {
  const MisTarjetas({super.key});

  @override
  State<MisTarjetas> createState() => _MisTarjetasState();
}

class _MisTarjetasState extends State<MisTarjetas> {
  List<TarjetaGuardada>? _tarjetas;
  String? _error;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    try {
      final t = await context.read<TarjetasRepository>().listar();
      if (mounted) {
        setState(() {
          _tarjetas = t;
          _error = null;
        });
      }
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.message);
    }
  }

  Future<void> _eliminar(TarjetaGuardada t) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar tarjeta'),
        content: Text('¿Eliminar ${t.descripcion}? Podrás agregarla de nuevo cuando quieras.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Eliminar')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    try {
      await context.read<TarjetasRepository>().eliminar(t.id);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Tarjeta eliminada')));
      _cargar();
    } on ApiException catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.message)));
    }
  }

  Future<void> _agregar() async {
    final guardada = await showModalBottomSheet<TarjetaGuardada>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => const _AgregarTarjetaSheet(),
    );
    if (guardada != null && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('${guardada.descripcion} guardada')));
      _cargar();
    }
  }

  @override
  Widget build(BuildContext context) {
    return SectionCard(
      title: 'Mis tarjetas',
      children: [
        Text('Tarjetas de débito para pagar tus servicios. Solo guardamos la marca, los últimos 4 dígitos y el vencimiento.',
            style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 8),
        if (_error != null) MessageBanner(message: _error!),
        if (_tarjetas == null && _error == null) const LinearProgressIndicator(),
        if (_tarjetas != null && _tarjetas!.isEmpty) const Text('Aún no tienes tarjetas guardadas.'),
        for (final t in _tarjetas ?? const <TarjetaGuardada>[])
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.credit_card),
            title: Text(t.descripcion),
            subtitle: Text(t.vencida ? 'Vencida (${t.vencimiento})' : 'Vence ${t.vencimiento}'),
            trailing: IconButton(
              tooltip: 'Eliminar',
              icon: const Icon(Icons.delete_outline),
              onPressed: () => _eliminar(t),
            ),
          ),
        const SizedBox(height: 4),
        OutlinedButton.icon(onPressed: _agregar, icon: const Icon(Icons.add_card), label: const Text('Agregar tarjeta')),
      ],
    );
  }
}

class _AgregarTarjetaSheet extends StatefulWidget {
  const _AgregarTarjetaSheet();

  @override
  State<_AgregarTarjetaSheet> createState() => _AgregarTarjetaSheetState();
}

class _AgregarTarjetaSheetState extends State<_AgregarTarjetaSheet> {
  final _form = GlobalKey<FormState>();
  final _c = TarjetaControllers();
  bool _guardando = false;
  String? _error;

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  Future<void> _guardar() async {
    if (!(_form.currentState?.validate() ?? false)) return;
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      final t = await context.read<TarjetasRepository>().guardar(_c.paraApi());
      if (mounted) Navigator.pop(context, t);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e.fullMessage);
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: ResponsiveBody(
          child: Form(
            key: _form,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Agregar tarjeta de débito', style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 12),
                FormularioTarjeta(controllers: _c),
                if (_error != null) ...[MessageBanner(message: _error!), const SizedBox(height: 12)],
                LoadingButton(label: 'Guardar tarjeta', icon: Icons.save_outlined, loading: _guardando, onPressed: _guardar),
                TextButton(onPressed: _guardando ? null : () => Navigator.pop(context), child: const Text('Cancelar')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
