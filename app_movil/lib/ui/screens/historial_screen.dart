import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../data/models/pago.dart';
import '../../state/catalogo_controller.dart';
import '../../state/historial_controller.dart';
import '../widgets/pago_tile.dart';
import '../widgets/state_views.dart';
import 'resultado_pago_screen.dart';

/// Historial de pagos del usuario autenticado (paginado, más reciente primero).
class HistorialScreen extends StatefulWidget {
  const HistorialScreen({super.key});

  @override
  State<HistorialScreen> createState() => _HistorialScreenState();
}

class _HistorialScreenState extends State<HistorialScreen> {
  final _scroll = ScrollController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<HistorialController>().cargarSiHaceFalta();
      // El catálogo trae los logotipos de cada proveedor.
      context.read<CatalogoController>().cargar();
    });
    _scroll.addListener(() {
      if (_scroll.position.pixels > _scroll.position.maxScrollExtent - 300) {
        context.read<HistorialController>().cargarMas();
      }
    });
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ctrl = context.watch<HistorialController>();
    final logos = context.watch<CatalogoController>().logosPorServicio;

    Widget lista;
    if (ctrl.cargando && ctrl.pagos.isEmpty) {
      lista = const LoadingView(message: 'Cargando tus pagos…');
    } else if (ctrl.error != null && ctrl.pagos.isEmpty) {
      lista = ErrorView(message: ctrl.error!.message, onRetry: ctrl.recargar);
    } else if (ctrl.pagos.isEmpty) {
      lista = RefreshIndicator(
        onRefresh: ctrl.recargar,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          children: const [
            SizedBox(height: 120),
            EmptyView(message: 'Aún no tienes pagos registrados.', icon: Icons.receipt_long_outlined),
          ],
        ),
      );
    } else {
      lista = RefreshIndicator(
        onRefresh: ctrl.recargar,
        child: ListView.builder(
          controller: _scroll,
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(12, 0, 12, 24),
          itemCount: ctrl.pagos.length + 1,
          itemBuilder: (context, i) {
            if (i == ctrl.pagos.length) {
              if (ctrl.cargandoMas) {
                return const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              return Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  ctrl.hayMas ? '' : '${ctrl.total} pago(s)',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              );
            }
            final pago = ctrl.pagos[i];
            final logo = logos[pago.idServicio];
            return Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 760),
                child: PagoTile(
                  pago: pago,
                  logoUrl: logo,
                  onTap: () => Navigator.of(context).push(MaterialPageRoute(
                    builder: (_) => ResultadoPagoScreen(pago: pago, logoUrl: logo, desdeHistorial: true),
                  )),
                ),
              ),
            );
          },
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Historial de pagos')),
      body: Column(
        children: [
          SizedBox(
            height: 52,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              children: [
                _filtro(context, ctrl, null, 'Todos'),
                for (final r in ResultadoPago.values) _filtro(context, ctrl, r, r.label),
              ],
            ),
          ),
          Expanded(child: lista),
        ],
      ),
    );
  }

  Widget _filtro(BuildContext context, HistorialController ctrl, ResultadoPago? r, String label) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: ChoiceChip(
        label: Text(label),
        selected: ctrl.filtro == r,
        onSelected: ctrl.cargando ? null : (_) => ctrl.cambiarFiltro(r),
      ),
    );
  }
}
