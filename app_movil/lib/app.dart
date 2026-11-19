import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'state/alertas_controller.dart';
import 'state/auth_controller.dart';
import 'state/catalogo_controller.dart';
import 'state/historial_controller.dart';
import 'state/nav_controller.dart';
import 'ui/screens/home_shell.dart';
import 'ui/screens/login_screen.dart';
import 'ui/widgets/state_views.dart';

class HasaniApp extends StatelessWidget {
  const HasaniApp({super.key});

  static ThemeData _theme(Brightness b) {
    final scheme = ColorScheme.fromSeed(seedColor: const Color(0xFF0B5FFF), brightness: b);
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: scheme.surfaceContainerHighest.withOpacity(0.4),
      ),
      cardTheme: CardThemeData(
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: scheme.outlineVariant),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Hasani Pagos',
      debugShowCheckedModeBanner: false,
      theme: _theme(Brightness.light),
      darkTheme: _theme(Brightness.dark),
      home: const _AuthGate(),
    );
  }
}

/// Decide qué mostrar según la sesión y arranca / detiene el aviso de nuevos dispositivos.
class _AuthGate extends StatefulWidget {
  const _AuthGate();

  @override
  State<_AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<_AuthGate> {
  AuthStatus? _anterior;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => context.read<AuthController>().iniciar());
  }

  void _alCambiar(AuthStatus status) {
    if (status == _anterior) return;
    _anterior = status;
    final alertas = context.read<AlertasController>();
    if (status == AuthStatus.authenticated) {
      alertas.iniciar();
    } else {
      alertas.detener();
      // Al salir no debe quedar información del usuario anterior.
      context.read<CatalogoController>().limpiar();
      context.read<HistorialController>().limpiar();
      context.read<NavController>().index = 0;
      Navigator.of(context).popUntil((r) => r.isFirst);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthController>();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _alCambiar(auth.status);
    });
    return switch (auth.status) {
      AuthStatus.checking => const Scaffold(body: LoadingView(message: 'Conectando con el servidor…')),
      AuthStatus.offline => Scaffold(
          body: ErrorView(
            message: 'No pudimos conectar con el servidor para recuperar tu sesión.',
            onRetry: auth.iniciar,
          ),
        ),
      AuthStatus.unauthenticated => const LoginScreen(),
      AuthStatus.authenticated => const HomeShell(),
    };
  }
}
