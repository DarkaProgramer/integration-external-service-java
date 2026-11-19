import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/network/api_exception.dart';
import '../../core/utils/validators.dart';
import '../../state/auth_controller.dart';
import '../widgets/app_text_field.dart';
import '../widgets/common.dart';
import '../widgets/state_views.dart';
import 'registro_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _form = GlobalKey<FormState>();
  final _correo = TextEditingController();
  final _password = TextEditingController();
  bool _enviando = false;
  String? _error;

  @override
  void dispose() {
    _correo.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _entrar() async {
    FocusScope.of(context).unfocus();
    if (!_form.currentState!.validate()) return;
    setState(() {
      _enviando = true;
      _error = null;
    });
    try {
      await context.read<AuthController>().login(_correo.text, _password.text);
      // AuthController cambia el estado y la app muestra el inicio.
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _error = e.statusCode == 401 ? 'Correo o contraseña incorrectos.' : e.fullMessage);
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final aviso = context.watch<AuthController>().avisoLogin;
    final theme = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          child: ResponsiveBody(
            maxWidth: 440,
            padding: const EdgeInsets.fromLTRB(24, 40, 24, 24),
            child: AutofillGroup(
              child: Form(
                key: _form,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Icon(Icons.account_balance_wallet_rounded, size: 64, color: theme.colorScheme.primary),
                    const SizedBox(height: 12),
                    Text('Hasani Pagos', textAlign: TextAlign.center, style: theme.textTheme.headlineMedium),
                    const SizedBox(height: 4),
                    Text('Paga tus servicios con Gestopago',
                        textAlign: TextAlign.center, style: theme.textTheme.bodyMedium),
                    const SizedBox(height: 32),
                    if (aviso != null && _error == null) ...[
                      MessageBanner(message: aviso, kind: BannerKind.warning),
                      const SizedBox(height: 16),
                    ],
                    if (_error != null) ...[
                      MessageBanner(message: _error!),
                      const SizedBox(height: 16),
                    ],
                    AppTextField(
                      controller: _correo,
                      label: 'Correo electrónico',
                      prefixIcon: Icons.mail_outline,
                      keyboardType: TextInputType.emailAddress,
                      autofillHints: const [AutofillHints.email, AutofillHints.username],
                      validator: Validators.email,
                      maxLength: 100,
                    ),
                    PasswordField(
                      controller: _password,
                      validator: Validators.loginPassword,
                      onSubmitted: (_) => _entrar(),
                    ),
                    const SizedBox(height: 8),
                    LoadingButton(label: 'Iniciar sesión', loading: _enviando, onPressed: _entrar),
                    const SizedBox(height: 16),
                    TextButton(
                      onPressed: _enviando
                          ? null
                          : () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const RegistroScreen())),
                      child: const Text('¿No tienes cuenta? Regístrate'),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Usa la misma cuenta en la web y en la app.',
                      textAlign: TextAlign.center,
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
