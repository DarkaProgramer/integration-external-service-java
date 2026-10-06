# Integración GestoPago – Consulta de productos

Consumo de `GET /sistema/service/getProductList.do` con autenticación Bearer.
Expuesto en la aplicación como `GET /gestopago/productos`.

## Archivos

| Capa | Archivo |
|---|---|
| Configuración | `application.properties`, `config/GestoPagoProductsFeignConfig` |
| Client/Integration | `client/GestoPagoProductsClient` (Feign) |
| Service | `service/GestoPagoProductService`, `service/Impl/GestoPagoProductServiceImpl` |
| Controller | `controller/GestoPagoProductController` |
| DTOs | `model/gestopago/GestoPagoProductListResponse`, `GestoPagoProduct` |
| Errores | `exception/GestoPagoErrorType`, `GestoPagoIntegrationException`, `GestoPagoExceptionHandler` |
| Pruebas | `GestoPagoProductServiceImplTest`, `GestoPagoProductsFeignConfigTest` |

## Propiedades

```properties
gestopago.products.url=https://gestopago.portalventas.net
gestopago.products.bearer-token=${GESTOPAGO_PRODUCTS_TOKEN:}
gestopago.products.connect-timeout-ms=5000
gestopago.products.read-timeout-ms=10000
```

Siguen la convención `gestopago.<recurso>.<propiedad>` (igual que `gestopago.auth.*`).
El token **no está en el código ni en el repositorio**: se lee de la variable de entorno
`GESTOPAGO_PRODUCTS_TOKEN` (o de Spring Cloud Config / secreto del orquestador).

## Decisiones técnicas

1. **Feign**, igual que `GestoPagoAuthClient`; no se introdujo otra librería HTTP.
2. **Bearer en un `RequestInterceptor`** (`GestoPagoProductsFeignConfig`): el token se agrega en un único punto,
   sin repetirlo en cada método. La clase no lleva `@Configuration` a propósito, para que
   aplique solo a este cliente y no a los demás Feign. Si el token falta, falla con error de autenticación
   sin hacer la llamada.
3. **Timeouts** por propiedades (`spring.cloud.openfeign.client.config.gestoPagoProducts.*`). Feign no reintenta por defecto.
4. **Traducción de errores en el servicio** a `GestoPagoIntegrationException` con un `GestoPagoErrorType`:

   | Situación | Tipo | HTTP devuelto |
   |---|---|---|
   | Falla de red / conexión (`RetryableException`) | `COMMUNICATION` (101) | 502 |
   | Timeout (causa `InterruptedIOException`/`HttpTimeoutException`) | `TIMEOUT` (102) | 504 |
   | 401 / 403 del servicio externo, o token no configurado | `AUTHENTICATION` (103) | 502 |
   | Otro código no 2xx | `UNSUCCESSFUL_RESPONSE` (104) | 502 |
   | Cuerpo nulo o no deserializable | `INVALID_RESPONSE` (105) | 502 |

   Un 401 del proveedor es un problema de configuración nuestro, no del consumidor, por eso se devuelve 502 y no 401.
   `GestoPagoExceptionHandler` (limitado a este controller) responde con `GenericResponse {codigo, mensaje}`.
5. **Logs**: inicio y fin de cada invocación con resultado y duración. En errores solo se registra tipo de error
   y clase de la causa; nunca el token, headers, URL, cuerpo ni `getMessage()` de la excepción original.
   Los mensajes enviados al consumidor son fijos (enum). **No activar `feign.Logger.Level.FULL`**: imprimiría el header Authorization.
6. **Inyección por constructor** en servicio y controller (el código existente usa `@Autowired` en campo; se prefirió
   constructor por testabilidad).

## Supuestos a validar

- **Estructura de la respuesta**: no se conoce el contrato real. Los DTOs asumen
  `{ "status", "message", "productList": [ { "id", "name", "description", "price" } ] }`.
  Tienen `ignoreUnknown = true`; ajustar nombres de campos al contrato real.
- Se asumió que el token Bearer es estático y viene de configuración, como pide el requerimiento. El proyecto ya
  tiene `GestoPagoTokenService` que renueva y guarda un token en BD; si el servicio de productos usa ese mismo token,
  se puede cambiar el interceptor para leerlo de allí sin tocar el resto de capas.

## Pruebas

`./gradlew test` – el servicio se prueba con el cliente simulado (Mockito): respuesta exitosa, cuerpo nulo,
401/403, 4xx/5xx, timeout, error de conexión, JSON inválido, propagación de errores del cliente y que
el mensaje interno no se exponga. Además se prueba el interceptor (header Bearer y token ausente).
