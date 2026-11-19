import 'package:flutter_test/flutter_test.dart';
import 'package:hasani_pagos/core/utils/device_name.dart';
import 'package:hasani_pagos/core/utils/validators.dart';

void main() {
  group('Validators', () {
    test('contraseña: 8-72, letra y número', () {
      expect(Validators.newPassword('abc12345'), isNull);
      expect(Validators.newPassword('abcdefgh'), isNotNull);
      expect(Validators.newPassword('1234567'), isNotNull);
    });

    test('correo', () {
      expect(Validators.email('ana@correo.com'), isNull);
      expect(Validators.email('ana@'), isNotNull);
    });

    test('referencia según el producto', () {
      expect(Validators.reference('5512345678', min: 10, max: 10, onlyDigits: true), isNull);
      expect(Validators.reference('55123', min: 10, max: 10, onlyDigits: true), isNotNull);
      expect(Validators.reference('55123A7890', min: 10, max: 10, onlyDigits: true), isNotNull);
      expect(Validators.reference('AB 12', min: 0, max: 0, onlyDigits: false), isNotNull);
      expect(Validators.reference('AB12', min: 0, max: 0, onlyDigits: false), isNull);
    });

    test('CURP y RFC', () {
      expect(Validators.curp('GOMA900101HDFRRN09'), isNull);
      expect(Validators.curp('XXXX'), isNotNull);
      expect(Validators.rfc('GOMA900101AB1'), isNull);
    });
  });

  test('nombre del dispositivo solo con caracteres permitidos', () {
    expect(sanitizeDeviceName('Pixel 8 Pro™ <x>'), 'Pixel 8 Pro x');
    expect(sanitizeDeviceName('!!!'), 'Móvil');
  });
}
