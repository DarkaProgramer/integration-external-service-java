import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../state/catalogo_controller.dart';
import '../widgets/producto_card.dart';
import '../widgets/state_views.dart';
import 'referencia_screen.dart';

class CatalogoScreen extends StatefulWidget {
  const CatalogoScreen({super.key});

  @override
  State<CatalogoScreen> createState() => _CatalogoScreenState();
}

class _CatalogoScreenState extends State<CatalogoScreen> {
  final _buscar = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<CatalogoController>().cargar());
    _buscar.text = context.read<CatalogoController>().busqueda;
  }

  @override
  void dispose() {
    _buscar.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final ctrl = context.watch<CatalogoController>();

    Widget body;
    if (ctrl.cargando && !ctrl.cargado) {
      body = const LoadingView(message: 'Consultando el catálogo de Gestopago…');
    } else if (ctrl.error != null && !ctrl.cargado) {
      body = ErrorView(
        message: ctrl.error!.statusCode == 503
            ? 'Gestopago no respondió. No podemos mostrar el catálogo en este momento.'
            : ctrl.error!.message,
        onRetry: () => ctrl.cargar(forzar: true),
      );
    } else {
      final productos = ctrl.visibles;
      body = RefreshIndicator(
        onRefresh: () => ctrl.cargar(forzar: true),
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
                child: TextField(
                  controller: _buscar,
                  onChanged: ctrl.buscar,
                  textInputAction: TextInputAction.search,
                  decoration: InputDecoration(
                    hintText: 'Buscar servicio o proveedor',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _buscar.text.isEmpty
                        ? null
                        : IconButton(
                            tooltip: 'Limpiar',
                            icon: const Icon(Icons.close),
                            onPressed: () {
                              _buscar.clear();
                              ctrl.buscar('');
                            },
                          ),
                  ),
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: SizedBox(
                height: 52,
                child: ListView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: ChoiceChip(
                        label: const Text('Todas'),
                        selected: ctrl.categoria == null,
                        onSelected: (_) => ctrl.filtrarCategoria(null),
                      ),
                    ),
                    for (final c in ctrl.categorias)
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: ChoiceChip(
                          label: Text(c),
                          selected: ctrl.categoria == c,
                          onSelected: (_) => ctrl.filtrarCategoria(ctrl.categoria == c ? null : c),
                        ),
                      ),
                  ],
                ),
              ),
            ),
            if (productos.isEmpty)
              const SliverFillRemaining(
                hasScrollBody: false,
                child: EmptyView(message: 'No hay productos que coincidan con la búsqueda.', icon: Icons.search_off),
              )
            else
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(12, 4, 12, 24),
                sliver: SliverGrid(
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 360,
                    mainAxisExtent: 196,
                    crossAxisSpacing: 4,
                    mainAxisSpacing: 4,
                  ),
                  delegate: SliverChildBuilderDelegate(
                    (context, i) => ProductoCard(
                      producto: productos[i],
                      onTap: () => Navigator.of(context).push(
                        MaterialPageRoute(builder: (_) => ReferenciaScreen(producto: productos[i])),
                      ),
                    ),
                    childCount: productos.length,
                  ),
                ),
              ),
          ],
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Catálogo de servicios'),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            onPressed: ctrl.cargando ? null : () => ctrl.cargar(forzar: true),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: body,
    );
  }
}
