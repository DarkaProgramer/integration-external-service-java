import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../state/nav_controller.dart';
import '../widgets/session_alert_listener.dart';
import 'catalogo_screen.dart';
import 'cuenta_screen.dart';
import 'historial_screen.dart';

/// Navegación principal: barra inferior en teléfonos y riel lateral en tabletas / horizontal.
class HomeShell extends StatelessWidget {
  const HomeShell({super.key});

  static const _destinos = [
    (Icons.storefront_outlined, Icons.storefront, 'Catálogo'),
    (Icons.receipt_long_outlined, Icons.receipt_long, 'Historial'),
    (Icons.person_outline, Icons.person, 'Cuenta'),
  ];

  @override
  Widget build(BuildContext context) {
    final nav = context.watch<NavController>();
    final paginas = const [CatalogoScreen(), HistorialScreen(), CuentaScreen()];
    final contenido = IndexedStack(index: nav.index, children: paginas);

    return SessionAlertListener(
      child: LayoutBuilder(
        builder: (context, box) {
          if (box.maxWidth >= 720) {
            return Scaffold(
              body: Row(
                children: [
                  SafeArea(
                    child: NavigationRail(
                      selectedIndex: nav.index,
                      onDestinationSelected: nav.ir,
                      labelType: NavigationRailLabelType.all,
                      leading: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: Icon(Icons.account_balance_wallet_rounded,
                            size: 32, color: Theme.of(context).colorScheme.primary),
                      ),
                      destinations: [
                        for (final d in _destinos)
                          NavigationRailDestination(icon: Icon(d.$1), selectedIcon: Icon(d.$2), label: Text(d.$3)),
                      ],
                    ),
                  ),
                  const VerticalDivider(width: 1),
                  Expanded(child: contenido),
                ],
              ),
            );
          }
          return Scaffold(
            body: contenido,
            bottomNavigationBar: NavigationBar(
              selectedIndex: nav.index,
              onDestinationSelected: nav.ir,
              destinations: [
                for (final d in _destinos)
                  NavigationDestination(icon: Icon(d.$1), selectedIcon: Icon(d.$2), label: d.$3),
              ],
            ),
          );
        },
      ),
    );
  }
}
