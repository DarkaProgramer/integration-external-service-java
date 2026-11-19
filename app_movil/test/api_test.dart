import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:hasani_pagos/core/network/api_client.dart';
import 'package:hasani_pagos/core/network/api_exception.dart';
import 'package:hasani_pagos/data/models/pago.dart';
import 'package:hasani_pagos/data/models/producto.dart';
import 'package:hasani_pagos/data/repositories/pagos_repository.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

http.Response _json(int status, Object body) =>
    http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json; charset=utf-8'});

const _producto = Producto(
  identificador: '1-100',
  idServicio: '1',
  idProducto: '100',
  nombre: 'Telcel \$100',
  servicio: 'Telcel',
  categoria: 'Recargas',
  precio: 100,
);

void main() {
  test('lee la envoltura {datos} y manda el Bearer', () async {
    final client = MockClient((req) async {
      expect(req.headers['Authorization'], 'Bearer abc');
      return _json(200, {'codigo': 0, 'mensaje': 'ok', 'status': 200, 'datos': [{'idServicio': '1'}]});
    });
    final api = ApiClient(httpClient: client, baseUrl: 'http://x')..setAccessToken('abc');
    final r = await api.get('/api/catalogo/productos');
    expect(r.data, isA<List>());
  });

  test('convierte errores en ApiException con detalles', () async {
    final client = MockClient((_) async => _json(400, {
          'mensaje': 'Datos inválidos',
          'detalles': {'correo': 'Correo inválido'},
        }));
    final api = ApiClient(httpClient: client, baseUrl: 'http://x');
    expect(
      () => api.post('/api/auth/login', auth: false, body: {}),
      throwsA(isA<ApiException>()
          .having((e) => e.statusCode, 'status', 400)
          .having((e) => e.details['correo'], 'detalle', 'Correo inválido')),
    );
  });

  test('ante un 401 renueva el token una vez y repite la petición', () async {
    var llamadas = 0;
    final client = MockClient((req) async {
      llamadas++;
      if (req.headers['Authorization'] == 'Bearer viejo') return _json(401, {'mensaje': 'expirado'});
      return _json(200, {'datos': 'ok'});
    });
    final api = ApiClient(httpClient: client, baseUrl: 'http://x')..setAccessToken('viejo');
    api.setRefresher(() async {
      api.setAccessToken('nuevo');
      return 'nuevo';
    });
    final r = await api.get('/api/auth/perfil');
    expect(r.data, 'ok');
    expect(llamadas, 2);
  });

  test('si el refresh falla avisa que la sesión expiró', () async {
    final client = MockClient((_) async => _json(401, {'mensaje': 'expirado'}));
    var expiro = false;
    final api = ApiClient(httpClient: client, baseUrl: 'http://x')
      ..setAccessToken('viejo')
      ..setRefresher(() async => null)
      ..onSessionExpired = () => expiro = true;
    await expectLater(api.get('/api/auth/perfil'), throwsA(isA<ApiException>()));
    expect(expiro, isTrue);
  });

  group('pagar', () {
    Future<Pago> pagarCon(http.Response respuesta, {void Function(http.Request)? revisar}) {
      final client = MockClient((req) async {
        revisar?.call(req);
        return respuesta;
      });
      final repo = PagosRepository(ApiClient(httpClient: client, baseUrl: 'http://x'));
      return repo.pagar(
        idempotencyKey: 'k-1',
        numeroCuenta: '0123456789',
        producto: _producto,
        referencia: '5512345678',
        validacionId: 'v-1',
        monto: 100,
        tarjetaGuardadaId: 'tok_1',
        cvv: '123',
      );
    }

    test('201 EXITOSO con folio e Idempotency-Key', () async {
      final p = await pagarCon(
        _json(201, {'datos': {'folio': 'f1', 'resultado': 'EXITOSO', 'folioProveedor': 'GP1', 'monto': 100}}),
        revisar: (req) {
          expect(req.headers['Idempotency-Key'], 'k-1');
          final body = jsonDecode(req.body) as Map;
          expect(body.keys.toSet(),
              {'numeroCuenta', 'idServicio', 'idProducto', 'referencia', 'validacionId', 'monto', 'tarjetaGuardadaId', 'cvv'});
          expect(body['tarjetaGuardadaId'], 'tok_1');
        },
      );
      expect(p.resultado, ResultadoPago.exitoso);
      expect(p.folioProveedor, 'GP1');
    });

    test('tarjeta nueva: manda la tarjeta y guardarTarjeta, sin tarjetaGuardadaId', () async {
      final client = MockClient((req) async {
        final body = jsonDecode(req.body) as Map;
        expect(body['tarjeta'], {'numero': '4000123456759010'});
        expect(body['guardarTarjeta'], true);
        expect(body.containsKey('tarjetaGuardadaId'), isFalse);
        return _json(201, {'datos': {'folio': 'f9', 'resultado': 'EXITOSO'}});
      });
      final repo = PagosRepository(ApiClient(httpClient: client, baseUrl: 'http://x'));
      await repo.pagar(
        idempotencyKey: 'k-9',
        numeroCuenta: '0123456789',
        producto: _producto,
        referencia: '5512345678',
        validacionId: 'v-9',
        monto: 100,
        tarjetaNueva: {'numero': '4000123456759010'},
        guardarTarjeta: true,
      );
    });

    test('422 con datos = RECHAZADO (no es excepción)', () async {
      final p = await pagarCon(_json(422, {'datos': {'folio': 'f2', 'resultado': 'RECHAZADO'}}));
      expect(p.resultado, ResultadoPago.rechazado);
    });

    test('202 PENDIENTE', () async {
      final p = await pagarCon(_json(202, {'datos': {'folio': 'f3', 'resultado': 'PENDIENTE'}}));
      expect(p.resultado, ResultadoPago.pendiente);
    });

    test('resultado desconocido nunca se muestra como exitoso', () {
      expect(ResultadoPago.fromApi(null), ResultadoPago.pendiente);
      expect(ResultadoPago.fromApi('RARO'), ResultadoPago.pendiente);
    });

    test('422 sin datos (validación vencida, etc.) es un error', () async {
      await expectLater(
        pagarCon(_json(422, {'mensaje': 'Referencia no validada'})),
        throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 422)),
      );
    });
  });

  test('Producto lee las reglas de la referencia', () {
    final p = Producto.fromJson({
      'identificador': '1-100',
      'idServicio': '1',
      'idProducto': '100',
      'nombre': 'Telcel \$100',
      'servicio': 'Telcel',
      'categoria': 'Recargas',
      'precio': 100.0,
      'montoVariable': false,
      'referencia': {'longitudMinima': 10, 'longitudMaxima': 10, 'soloDigitos': true},
    });
    expect(p.refLongitudMinima, 10);
    expect(p.refSoloDigitos, isTrue);
    expect(p.referenciaHint, '10 dígitos');
  });
}
