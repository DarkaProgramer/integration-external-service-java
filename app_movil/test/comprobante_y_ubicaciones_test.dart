import 'package:flutter_test/flutter_test.dart';
import 'package:hasani_pagos/core/utils/comprobante_pdf.dart';
import 'package:hasani_pagos/core/utils/logos.dart';
import 'package:hasani_pagos/data/models/pago.dart';
import 'package:hasani_pagos/data/repositories/ubicaciones_repository.dart';
import 'package:intl/date_symbol_data_local.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUpAll(() => initializeDateFormatting('es_MX'));

  test('el comprobante PDF se genera con los datos del pago', () async {
    final pago = Pago.fromJson({
      'folio': 'f-1',
      'resultado': 'EXITOSO',
      'servicio': 'Telcel',
      'referencia': '5512345678',
      'monto': 100,
      'tarjeta': 'Visa •••• 9010',
      'autorizacion': 'A1',
      'fechaOperacion': '2026-11-19T10:00:00',
    });
    expect(pago.tarjeta, 'Visa •••• 9010');
    final bytes = await ComprobantePdf.generar(pago);
    expect(String.fromCharCodes(bytes.take(5)), '%PDF-');
  });

  test('catálogo de ubicaciones: países con México primero y municipios por estado', () async {
    final repo = UbicacionesRepository();
    final paises = await repo.paises();
    expect(paises.first, 'México');
    expect(paises, contains('Argentina'));
    final estados = await repo.estadosDeMexico();
    expect(estados, hasLength(32));
    expect(estados.firstWhere((e) => e.nombre == 'Aguascalientes').municipios, contains('Calvillo'));
    expect(UbicacionesRepository.esMexico(' méxico '), isTrue);
  });

  test('logos: mismas marcas que la web', () {
    expect(Logos.asset(['Telcel']), 'assets/logos/telcel.png');
    expect(Logos.asset(['Recibo', 'CFE Luz']), 'assets/logos/cfe.png');
    expect(Logos.asset(['Proveedor desconocido']), isNull);
  });
}
