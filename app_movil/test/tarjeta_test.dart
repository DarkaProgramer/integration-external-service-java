import 'package:flutter_test/flutter_test.dart';
import 'package:hasani_pagos/core/utils/tarjeta.dart';

void main() {
  group('TarjetaUtils', () {
    test('acepta débito Visa y Mastercard con dígito verificador válido', () {
      expect(TarjetaUtils.validarNumero('4000 1234 5675 9010'), isNull);
      expect(TarjetaUtils.validarNumero('5200 1234 5675 4321'), isNull);
      expect(TarjetaUtils.marca('5200123456754321'), 'MASTERCARD');
    });

    test('rechaza número inválido, AMEX y marcas desconocidas', () {
      expect(TarjetaUtils.validarNumero('4000 1234 5675 9011'), isNotNull);
      expect(TarjetaUtils.validarNumero('3782 822463 10005'), contains('American Express'));
      expect(TarjetaUtils.validarNumero('6011 1111 1111 1117'), isNotNull);
      expect(TarjetaUtils.validarNumero(''), isNotNull);
    });

    test('vencimiento con formato MM/AA y no vencida', () {
      final hoy = DateTime(2026, 11, 19);
      expect(TarjetaUtils.validarVencimiento('12/28', hoy), isNull);
      expect(TarjetaUtils.validarVencimiento('11/26', hoy), isNull);
      expect(TarjetaUtils.validarVencimiento('10/26', hoy), 'La tarjeta está vencida');
      expect(TarjetaUtils.validarVencimiento('13/28', hoy), 'Usa el formato MM/AA');
    });

    test('convierte al formato de la API', () {
      final j = TarjetaUtils.paraApi(numero: '4000 1234 5675 9010', titular: ' ANA LOPEZ ', vencimiento: '12/28', cvv: '123');
      expect(j, {'numero': '4000123456759010', 'titular': 'ANA LOPEZ', 'mesVencimiento': 12, 'anioVencimiento': 2028, 'cvv': '123'});
    });
  });
}
