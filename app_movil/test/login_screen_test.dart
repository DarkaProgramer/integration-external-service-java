import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:hasani_pagos/core/network/api_client.dart';
import 'package:hasani_pagos/core/storage/token_storage.dart';
import 'package:hasani_pagos/data/repositories/auth_repository.dart';
import 'package:hasani_pagos/state/auth_controller.dart';
import 'package:hasani_pagos/ui/screens/login_screen.dart';
import 'package:provider/provider.dart';

void main() {
  testWidgets('el login valida el formulario antes de llamar al API', (tester) async {
    final api = ApiClient(baseUrl: 'http://localhost');
    final auth = AuthController(AuthRepository(api, TokenStorage()), api)..status = AuthStatus.unauthenticated;

    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: auth,
        child: const MaterialApp(home: LoginScreen()),
      ),
    );

    expect(find.text('Hasani Pagos'), findsOneWidget);
    await tester.tap(find.text('Iniciar sesión'));
    await tester.pump();

    expect(find.text('El correo es obligatorio'), findsOneWidget);
    expect(find.text('La contraseña es obligatoria'), findsOneWidget);
  });
}
