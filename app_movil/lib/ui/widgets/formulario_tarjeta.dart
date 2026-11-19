import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../core/utils/tarjeta.dart';
import 'app_text_field.dart';

/// Controladores de una tarjeta nueva. Los datos solo viven en memoria mientras se usa el formulario.
class TarjetaControllers {
  final numero = TextEditingController();
  final titular = TextEditingController();
  final vencimiento = TextEditingController();
  final cvv = TextEditingController();

  Map<String, dynamic> paraApi() => TarjetaUtils.paraApi(
        numero: numero.text,
        titular: titular.text,
        vencimiento: vencimiento.text,
        cvv: cvv.text,
      );

  void limpiar() {
    for (final c in [numero, titular, vencimiento, cvv]) {
      c.clear();
    }
  }

  void dispose() {
    for (final c in [numero, titular, vencimiento, cvv]) {
      c.dispose();
    }
  }
}

const _tarjetasPrueba = [
  ('4000 1234 5675 9010', 'Visa débito: aprobada'),
  ('5200 1234 5675 4321', 'Mastercard débito: aprobada'),
  ('4000 1234 5674 0002', 'Débito: fondos insuficientes'),
  ('4111 1111 1111 1111', 'Crédito: no se acepta'),
];

/// Campos de una tarjeta de débito nueva con vista previa (igual que la web). Va dentro de un [Form].
class FormularioTarjeta extends StatefulWidget {
  const FormularioTarjeta({super.key, required this.controllers});

  final TarjetaControllers controllers;

  @override
  State<FormularioTarjeta> createState() => _FormularioTarjetaState();
}

class _FormularioTarjetaState extends State<FormularioTarjeta> {
  TarjetaControllers get c => widget.controllers;

  void _refrescar() => setState(() {});

  @override
  void initState() {
    super.initState();
    for (final t in [c.numero, c.titular, c.vencimiento]) {
      t.addListener(_refrescar);
    }
  }

  @override
  void dispose() {
    for (final t in [c.numero, c.titular, c.vencimiento]) {
      t.removeListener(_refrescar);
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final m = TarjetaUtils.marca(c.numero.text);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _VistaPrevia(numero: c.numero.text, titular: c.titular.text, vencimiento: c.vencimiento.text, marca: m),
        const SizedBox(height: 16),
        AppTextField(
          controller: c.numero,
          label: 'Número de tarjeta${m != null && m != 'AMEX' ? ' · $m' : ''}',
          hint: '0000 0000 0000 0000',
          helper: 'Solo tarjetas de débito Visa o Mastercard',
          keyboardType: TextInputType.number,
          prefixIcon: Icons.credit_card,
          inputFormatters: [NumeroTarjetaFormatter()],
          autofillHints: const [AutofillHints.creditCardNumber],
          validator: TarjetaUtils.validarNumero,
        ),
        AppTextField(
          controller: c.titular,
          label: 'Nombre del titular',
          hint: 'Como aparece en la tarjeta',
          textCapitalization: TextCapitalization.characters,
          autofillHints: const [AutofillHints.creditCardName],
          validator: TarjetaUtils.validarTitular,
        ),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: AppTextField(
                controller: c.vencimiento,
                label: 'Vencimiento',
                hint: 'MM/AA',
                keyboardType: TextInputType.number,
                inputFormatters: [VencimientoFormatter()],
                autofillHints: const [AutofillHints.creditCardExpirationDate],
                validator: (v) => TarjetaUtils.validarVencimiento(v),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: AppTextField(
                controller: c.cvv,
                label: 'CVV',
                hint: '123',
                obscure: true,
                keyboardType: TextInputType.number,
                inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(3)],
                autofillHints: const [AutofillHints.creditCardSecurityCode],
                validator: TarjetaUtils.validarCvv,
              ),
            ),
          ],
        ),
        ExpansionTile(
          tilePadding: EdgeInsets.zero,
          title: Text('Tarjetas de prueba (no se cobra dinero real)', style: Theme.of(context).textTheme.bodyMedium),
          children: [
            for (final (n, d) in _tarjetasPrueba)
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                title: Text(n, style: const TextStyle(fontFamily: 'monospace')),
                subtitle: Text(d),
                trailing: const Icon(Icons.input),
                onTap: () => c.numero.text = n,
              ),
            const Padding(
              padding: EdgeInsets.only(bottom: 8),
              child: Text('Usa cualquier titular, una fecha futura y cualquier CVV de 3 dígitos.'),
            ),
          ],
        ),
      ],
    );
  }
}

class _VistaPrevia extends StatelessWidget {
  const _VistaPrevia({required this.numero, required this.titular, required this.vencimiento, required this.marca});

  final String numero;
  final String titular;
  final String vencimiento;
  final String? marca;

  @override
  Widget build(BuildContext context) {
    final digitos = TarjetaUtils.soloDigitos(numero).padRight(16, '•');
    final texto = digitos.replaceAllMapped(RegExp(r'(.{4})(?=.)'), (m) => '${m[1]} ');
    final colores = switch (marca) {
      'VISA' => const [Color(0xFF1A1F71), Color(0xFF2B4BB3)],
      'MASTERCARD' => const [Color(0xFF242424), Color(0xFFB4451F)],
      _ => const [Color(0xFF3F3D56), Color(0xFF6B6893)],
    };
    const blanco = TextStyle(color: Colors.white);
    return ExcludeSemantics(
      child: AspectRatio(
        aspectRatio: 1.586,
        child: Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            gradient: LinearGradient(colors: colores, begin: Alignment.topLeft, end: Alignment.bottomRight),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 40,
                    height: 30,
                    decoration: BoxDecoration(color: const Color(0xFFE6C065), borderRadius: BorderRadius.circular(6)),
                  ),
                  const Spacer(),
                  const Text('Débito', style: blanco),
                ],
              ),
              const Spacer(),
              FittedBox(
                child: Text(texto,
                    style: blanco.copyWith(fontSize: 22, letterSpacing: 2, fontFamily: 'monospace')),
              ),
              const Spacer(),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: Text(titular.isEmpty ? 'NOMBRE APELLIDO' : titular.toUpperCase(),
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: blanco),
                  ),
                  Text(vencimiento.isEmpty ? 'MM/AA' : vencimiento, style: blanco),
                  const SizedBox(width: 12),
                  Text(marca == 'VISA' ? 'VISA' : marca == 'MASTERCARD' ? 'mastercard' : '',
                      style: blanco.copyWith(fontWeight: FontWeight.w800, fontStyle: FontStyle.italic)),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
