import 'package:flutter/material.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:provider/provider.dart';

import 'app.dart';
import 'core/network/api_client.dart';
import 'core/storage/token_storage.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/catalogo_repository.dart';
import 'data/repositories/pagos_repository.dart';
import 'data/repositories/tarjetas_repository.dart';
import 'state/alertas_controller.dart';
import 'state/auth_controller.dart';
import 'state/catalogo_controller.dart';
import 'state/historial_controller.dart';
import 'state/nav_controller.dart';

/// Capas: core (red, almacenamiento, utilidades) → data (modelos y repositorios) → state → ui.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting('es_MX');

  final api = ApiClient();
  final authRepo = AuthRepository(api, TokenStorage());
  final catalogoRepo = CatalogoRepository(api);
  final pagosRepo = PagosRepository(api);
  final tarjetasRepo = TarjetasRepository(api);

  runApp(
    MultiProvider(
      providers: [
        Provider.value(value: authRepo),
        Provider.value(value: catalogoRepo),
        Provider.value(value: pagosRepo),
        Provider.value(value: tarjetasRepo),
        ChangeNotifierProvider(create: (_) => AuthController(authRepo, api)),
        ChangeNotifierProvider(create: (_) => AlertasController(authRepo)),
        ChangeNotifierProvider(create: (_) => CatalogoController(catalogoRepo)),
        ChangeNotifierProvider(create: (_) => HistorialController(pagosRepo)),
        ChangeNotifierProvider(create: (_) => NavController()),
      ],
      child: const HasaniApp(),
    ),
  );
}
