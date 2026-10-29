# Integración de pagos de servicios con Gestopago

Pago de servicios (recargas, recibos) desde la cuenta del cliente, integrado con
`POST https://gestopago.portalventas.net/sistema/service/sendTx.do`.

El objetivo de diseño es que **nunca se cobre dos veces y nunca se devuelva dinero de un pago que el
proveedor sí aplicó**, aun con timeouts, caídas del servidor o peticiones simultáneas.

## 1. Flujo completo

```
Cliente ──► POST /api/pagos/servicios
            Authorization: Bearer <JWT del cliente>
            Idempotency-Key: <UUID>
            { numeroCuenta, idServicio, idProducto, referencia, monto }

 1. Autenticación        JWT HS256 válido, vigente, del emisor correcto            → 401
 2. Autorización         el token trae el permiso pagos:servicios                   → 403
 3. Formato              Bean Validation del DTO; campos desconocidos rechazados    → 400
 4. Idempotencia         llave ya usada: mismo resultado (200) / otros datos (422)
 5. Catálogo             servicio+producto existen; monto = precio o dentro de rango → 422 / 503
 ─── Transacción 1 (cuenta bloqueada con SELECT ... FOR UPDATE) ───────────────────
 6. Titularidad          la cuenta es del cliente del token                         → 403
 7. Estado               cliente activo, cuenta ACTIVA                              → 403 / 422
 8. Duplicados           mismo pago reciente con otra llave (doble clic)            → 409
 9. Límite diario        suma del día + monto ≤ límite                              → 422
10. Saldo                saldo ≥ monto                                              → 422
11. Cargo                saldo -= monto; pago PENDIENTE; movimiento CARGO
 ─── Fuera de transacción ──────────────────────────────────────────────────────────
12. sendTx.do            una sola llamada, sin reintentos automáticos
 ─── Transacción 2 (pago bloqueado) ────────────────────────────────────────────────
13. Resultado            APROBADA → 201
                         RECHAZADA → devolución (movimiento REVERSO) → 422
                         INDETERMINADA → EN_CONCILIACION, dinero retenido → 202
```

Los pasos 1 a 10 no tienen efectos: si cualquiera falla no hay cargo, registro ni llamada al proveedor.

### Endpoints

| Método y ruta | Uso | Permiso |
|---|---|---|
| `POST /api/pagos/servicios` | Pagar | `pagos:servicios` |
| `POST /api/pagos/servicios/consultar` `{folio}` | Ver el estatus de un pago propio | `pagos:servicios` |
| `POST /api/pagos/servicios/conciliacion/resolver` `{folio, resultado, folioProveedor}` | Operador resuelve un pago dudoso | `pagos:conciliar` |
| `POST /api/dev/token` `{clienteId}` | Solo perfil `dev`: emite un token de prueba | — |

(Se mantiene la convención del proyecto: datos en el cuerpo, sin `@PathVariable`/`@RequestParam`.)

## 2. Estrategia de descuento: reservar, enviar y compensar solo con certeza

Se compararon tres alternativas:

| Estrategia | Problema |
|---|---|
| Llamar a Gestopago y descontar después | Si el descuento falla (saldo gastado por otra operación, caída del servidor) el pago ya quedó aplicado sin fondos. |
| Llamar a Gestopago dentro de la misma transacción del descuento | La fila de la cuenta queda bloqueada durante toda la llamada externa (hasta 20 s); un timeout hace rollback del cargo aunque el proveedor sí haya cobrado. |
| **Descontar (reservar) primero, llamar fuera de la transacción y compensar solo si hay certeza** | Es la elegida. El saldo nunca se usa dos veces y el dinero queda retenido mientras el resultado sea incierto. |

La devolución (REVERSO) solo ocurre cuando **hay certeza** de que el proveedor no procesó el pago:
rechazo explícito, HTTP que indica que la petición se rechazó antes de procesarse, o error de conexión
que prueba que la petición nunca salió.

## 3. Escenarios de fallo y cómo se manejan

| Escenario | Resultado | Dinero |
|---|---|---|
| Gestopago responde código de éxito | APROBADA (201) | descontado |
| Código de rechazo conocido (`gestopago.tx.codigos-rechazo`) | RECHAZADA (422) | devuelto |
| HTTP 400, 401, 403, 404, 405, 415, 422, 429 | RECHAZADA | devuelto |
| Conexión rechazada, host desconocido, token sin configurar | RECHAZADA (la petición no salió) | devuelto |
| **Timeout de lectura** | EN_CONCILIACION (202) | **retenido** |
| Conexión cortada a mitad, HTTP 408/409/5xx | EN_CONCILIACION | retenido |
| Respuesta ilegible, sin código o con código desconocido | EN_CONCILIACION | retenido |
| El servidor se cae entre el cargo y la respuesta | queda PENDIENTE; el job lo pasa a conciliación | retenido |
| Catálogo de Gestopago caído | 503, no se procesa (falla cerrada) | sin cargo |
| El cliente reintenta tras perder la respuesta | misma llave → mismo resultado, sin nuevo cargo | — |
| Doble clic con otra llave | 409 | sin cargo extra |
| Dos pagos simultáneos que juntos exceden el saldo | solo uno pasa (bloqueo de fila) | correcto |
| Misma llave enviada dos veces a la vez | una se procesa, la otra repite el resultado | un solo cargo |

### Por qué un timeout no se trata como fallo

Un timeout solo dice que **no llegó la respuesta**; la petición pudo haberse procesado. Si se devolviera el
dinero o se reintentara, se podría regalar el servicio o cobrar dos veces. Por eso:

1. **No hay reintentos automáticos** (`Retryer.NEVER_RETRY` en el cliente Feign de transacciones).
2. El pago pasa a `EN_CONCILIACION` con el dinero retenido y el cliente recibe 202 con su folio.
3. Un job (`ConciliacionPagosJob`, cada minuto) consulta el estado al proveedor mediante
   `ProveedorPagosGateway.consultarEstado`. Si hay respuesta concluyente, aplica APROBADA o RECHAZADA.
4. Si tras `pagos.conciliacion.max-intentos` no hay respuesta, el pago pasa a `REVISION_MANUAL` y un operador
   lo resuelve con el reporte o portal de Gestopago (`/conciliacion/resolver`). La restricción única
   `(pago_id, tipo)` en `movimientos_cuenta` impide devolver dos veces.

**Limitación actual:** no hay documentación pública de un endpoint de consulta de transacciones de
Gestopago, así que `consultarEstado` devuelve vacío y los casos dudosos terminan en revisión manual. Cuando
el proveedor lo proporcione, se implementa en `GestoPagoPagosGateway` y la conciliación se vuelve automática
sin tocar el resto del código. Además, a cada envío se agrega nuestro folio (`idTransaccionCliente`) para que,
si el proveedor lo soporta, pueda detectar reenvíos y se pueda buscar la operación en sus reportes.

## 4. Idempotencia y prevención de duplicados

* **Idempotency-Key** (UUID, obligatoria): el cliente la genera una vez por operación y la reutiliza en sus
  reintentos. Restricción única `(cliente_id, idempotency_key)` en base de datos.
* **Huella SHA-256** de cuenta+servicio+producto+referencia+monto: la misma llave con otros datos es 422.
* **Ventana anti duplicados** (`pagos.ventana-duplicados-segundos`, 120 s): el mismo pago con otra llave es 409.
* La verificación se repite con la cuenta bloqueada, y si dos peticiones con la misma llave chocan, la
  restricción única deja pasar solo una (`DataIntegrityViolationException` → se devuelve el resultado ganador).

## 5. Concurrencia

* **Bloqueo pesimista** de la fila de la cuenta (`findByNumeroCuentaParaActualizar`, `PESSIMISTIC_WRITE`) en
  la transacción del cargo: dos pagos simultáneos se serializan y ninguno lee un saldo desactualizado.
* **Bloqueo del pago** al aplicar el resultado: la petición original, el job y el operador no pueden aplicar
  resultados distintos; un pago final ya no cambia.
* `@Version` en `PagoServicio` como defensa adicional.
* Transacciones cortas: el bloqueo nunca se mantiene durante la llamada HTTP.
* Probado en `PagoServicioIntegrationTest` (`concurrenciaSaldo`, `concurrenciaMismaLlave`).

## 6. Seguridad

* **Token Bearer de Gestopago**: solo por variable de entorno (`GESTOPAGO_TX_TOKEN`). Se agrega en un
  `RequestInterceptor` exclusivo del cliente; si falta, la petición no sale. El log de Feign está en `NONE`
  (en `FULL` imprimiría el encabezado `Authorization`).
* **Contraseña de Gestopago**: se movió de `application.properties` a `GESTOPAGO_PASSWORD`. Como estuvo
  versionada, **debe cambiarse** con el proveedor. Además se corrigió un log que imprimía el mensaje de Feign
  del login, que incluye la URL con la contraseña como parámetro.
* **Identidad del cliente**: JWT HS256 (`APP_JWT_SECRET`, mínimo 32 caracteres) con emisor, sujeto (id del
  cliente) y expiración obligatorios. El `clienteId` nunca se toma del cuerpo.
* **No se revelan datos**: cuenta ajena e inexistente responden igual (403); el mensaje de saldo insuficiente
  no muestra el saldo; la referencia se devuelve enmascarada (`******5678`); los errores del proveedor se
  traducen a mensajes fijos.
* **Logs**: solo folio, cliente, ids de servicio/producto, monto, estatus y código. Nunca token, referencia
  completa, cuerpo de la respuesta ni `getMessage()` de excepciones externas.
* **XML seguro**: las respuestas XML se procesan con DTD y entidades externas deshabilitadas (XXE).
* **Montos manipulados**: el precio sale del catálogo del proveedor, no del cliente; `@Digits` impide más de
  2 decimales y se rechazan campos extra en el JSON (no se puede enviar `estatus`, `folio`, etc.).

## 7. Arquitectura (SOLID)

| Capa | Clases |
|---|---|
| Controller | `PagoServicioController` (solo HTTP: status según estatus, encabezados) |
| Seguridad | `TokenClienteService`, `ClienteActualArgumentResolver`, `@ClienteActual`, `ClienteAutenticado` |
| Servicio (orquestación) | `PagoServicioServiceImpl` |
| Servicio (transacciones) | `PagoServicioTransacciones` (bean aparte para que `@Transactional` aplique) |
| Conciliación | `ConciliacionPagosServiceImpl`, `ConciliacionPagosJob` |
| Puertos | `ProveedorPagosGateway`, `CatalogoProductosService` |
| Adaptadores Gestopago | `GestoPagoPagosGateway`, `GestoPagoCatalogoProductosService`, `RespuestaGestoPago`, `GestoPagoTransaccionesClient` |
| Repositorios | `PagoServicioRepository`, `MovimientoCuentaRepository`, `CuentaRepository` (lecturas con bloqueo) |
| Entidades | `PagoServicio`, `MovimientoCuenta`, `EstatusPago`, `TipoMovimiento` |
| Errores | `PagoException` y subclases, `PagoServicioExceptionHandler` (`@RestControllerAdvice`) |

* **Inversión de dependencias**: el flujo depende de `ProveedorPagosGateway`, no de Gestopago. El formato
  de sendTx.do está aislado en un solo adaptador.
* **Abierto/cerrado**: cada error define su status y mensaje; el `@RestControllerAdvice` tiene un único
  método para todos (`PagoException`), así agregar un error no modifica el manejador.
* **Responsabilidad única**: validar catálogo, mover dinero, hablar con el proveedor y conciliar están en
  clases distintas.

### Respuestas HTTP

| Código | Cuándo |
|---|---|
| 201 | Pago aprobado |
| 200 | Repetición con la misma Idempotency-Key (`Idempotent-Replayed: true`) |
| 202 | Resultado pendiente de confirmar con el proveedor (dinero retenido) |
| 400 | Formato inválido, campos extra, falta Idempotency-Key o no es UUID |
| 401 | Sin token, token inválido o expirado (`WWW-Authenticate: Bearer`) |
| 403 | Sin permiso, cuenta ajena o inexistente, cliente dado de baja |
| 404 | Folio inexistente o de otro cliente |
| 409 | Pago duplicado reciente; resolución manual de un pago ya final |
| 422 | Producto inexistente, monto no permitido, saldo insuficiente, límite diario, cuenta inactiva, llave reutilizada, rechazo del proveedor |
| 503 | Catálogo de Gestopago no disponible (sin cargo) |
| 500 | Error no previsto, con mensaje genérico |

## 8. Configuración

Variables de entorno (ninguna se versiona; ver `.env.example`):

| Variable | Uso |
|---|---|
| `GESTOPAGO_TX_TOKEN` | Token Bearer de sendTx.do (si falta, se usa `GESTOPAGO_PRODUCTS_TOKEN`) |
| `GESTOPAGO_PRODUCTS_TOKEN` | Token Bearer del catálogo |
| `GESTOPAGO_PASSWORD` | Contraseña del login de Gestopago |
| `APP_JWT_SECRET` | Secreto para firmar/validar los tokens de los clientes (≥ 32 caracteres) |
| `GESTOPAGO_CODIGOS_APROBADA` / `GESTOPAGO_CODIGOS_RECHAZO` | Códigos de respuesta del proveedor (por defecto `0,00` y ninguno) |

Reglas en `application.properties`: `pagos.monto-minimo`, `pagos.monto-maximo` (productos de monto
variable), `pagos.limite-diario`, `pagos.ventana-duplicados-segundos`, `pagos.catalogo.ttl-minutos`,
`pagos.conciliacion.*`. Timeouts: conexión 5 s, lectura 20 s.

Las tablas `pagos_servicio` y `movimientos_cuenta` las crea Hibernate (`ddl-auto=update`), igual que el resto
del proyecto.

## 9. Supuestos a validar con el proveedor

No hay documentación pública de `sendTx.do`. Todo lo siguiente está aislado en `GestoPagoPagosGateway`
y `GestoPagoTransaccionesClient`:

* Parámetros enviados como formulario: `idServicio`, `idProducto`, `referencia`, `montoPago`,
  `idTransaccionCliente`.
* La respuesta (XML o JSON) trae un código en `CODIGO`/`codigo` y el folio en `TRANSACCION`/`folio`/
  `NUM_AUTORIZACION`; `0`/`00` es éxito.
* Mientras no se conozca la tabla de códigos de rechazo, **todo código distinto de éxito va a conciliación**
  (opción segura: retiene el dinero en lugar de devolverlo a ciegas).
* El catálogo se lee del XML de `getProductList.do` (`producto` con `idServicio`, `idProducto`, `precio`);
  precio 0 se interpreta como monto variable.

## 10. Cómo probar

```bash
./gradlew test        # 283 pruebas (102 de esta integración)
```

* `PagoServicioIntegrationTest`: flujo completo con base H2 y un servidor HTTP local que simula Gestopago
  (aprobado, rechazo con devolución, timeout, 503 → revisión manual → resolución, idempotencia, duplicados,
  montos manipulados, cuenta ajena/inactiva, autenticación, concurrencia).
* Unitarias: `GestoPagoPagosGatewayTest` (clasificación de cada respuesta/error), `GestoPagoCatalogoProductosServiceTest`,
  `TokenClienteServiceTest`, `PagoServicioServiceImplTest`, `ConciliacionPagosServiceImplTest`,
  `PagoServicioControllerTest`, `RespuestaGestoPagoTest` (incluye XXE).

Prueba manual (perfil dev):

```bash
export APP_JWT_SECRET='un-secreto-largo-de-al-menos-32-caracteres'
export GESTOPAGO_TX_TOKEN='...'          # el token que entrega Gestopago
./gradlew bootRun --args='--spring.profiles.active=dev'

TOKEN=$(curl -s -X POST localhost:8081/api/dev/token -H 'Content-Type: application/json' \
  -d '{"clienteId":1}' | jq -r .datos.accessToken)

curl -i -X POST localhost:8081/api/pagos/servicios \
  -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $(uuidgen)" -H 'Content-Type: application/json' \
  -d '{"numeroCuenta":"<cuenta del cliente 1>","idServicio":"1","idProducto":"100","referencia":"5512345678","monto":100.00}'
```
