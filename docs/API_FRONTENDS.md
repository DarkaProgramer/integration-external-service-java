# API para la web y la app Flutter

Guía de los endpoints que usan los dos frontends. Los dos consumen **exactamente la misma API**, con las mismas
credenciales. Todas las respuestas usan la estructura `{codigo, mensaje, timestamp, status, datos}`; los errores usan
`{status, error, mensaje, ruta, detalles}`.

Base URL: `https://hasani-api.onrender.com` (local: `http://localhost:8081`). Swagger: `/swagger-ui.html`.

## Flujo completo

```
registro / login ──► catálogo ──► validar referencia ──► pagar ──► comprobante ──► historial / detalle
        │                                                             (EXITOSO / RECHAZADO / PENDIENTE)
        └─► cada 20 s: GET /api/auth/alertas  (aviso si la cuenta entra en otro dispositivo)
```

## 1. Sesión

| Método y ruta | Token | Uso |
|---|---|---|
| `POST /api/auth/registro` | — | Alta: datos del cliente + `password` + `dispositivo`. Abre la primera sesión (201). |
| `POST /api/auth/login` | — | `{correo, password, dispositivo:{tipo:"WEB"\|"MOVIL", nombre}}` |
| `POST /api/auth/refresh` | — | `{refreshToken}` → tokens nuevos (el refresh anterior deja de servir). |
| `POST /api/auth/logout` | Bearer | Cierra la sesión de este dispositivo. |
| `GET /api/auth/perfil` | Bearer | Nombre, correo, `numeroCuenta` (el saldo ya no se usa para pagar). |
| `GET /api/auth/sesiones` | Bearer | Sesiones abiertas (`actual: true` = este dispositivo). |
| `POST /api/auth/sesiones/cerrar` | Bearer | `{sesionId}`: cerrar otro dispositivo ("no fui yo"). |
| `GET /api/auth/alertas` | Bearer | Alertas no leídas: la cuenta inició sesión en otro dispositivo. |
| `POST /api/auth/alertas/leidas` | Bearer | Marca como leídas las alertas de este dispositivo. |

Respuesta de registro/login/refresh (`datos`):

```json
{
  "tokenType": "Bearer",
  "accessToken": "eyJ...",          // 15 minutos. Encabezado: Authorization: Bearer <accessToken>
  "expiraEn": 900,
  "refreshToken": "q8x...",          // 30 días, rota en cada uso
  "refreshExpiraEn": 2592000,
  "sesionId": "uuid",
  "perfil": { "nombre": "Ana", "correo": "...", "numeroCuenta": "0123456789", "saldo": 1000.00 },
  "otrasSesionesActivas": [ { "sesionId": "...", "tipoDispositivo": "WEB", "nombreDispositivo": "Chrome" } ]
}
```

Reglas:

* Web y móvil pueden estar abiertos **a la vez**. Al iniciar sesión en un dispositivo nuevo, los que ya estaban
  abiertos reciben una alerta en `GET /api/auth/alertas` (hacer polling cada ~20 s mientras la app está abierta y
  mostrar un aviso con opción "Cerrar esa sesión").
* Un `401` en cualquier endpoint protegido: intentar `POST /api/auth/refresh`; si también da 401, mandar al login.
* Guardar el refreshToken en almacenamiento seguro (Flutter: `flutter_secure_storage`; web: memoria o
  `sessionStorage`, nunca en el código).
* Login: 401 con mensaje genérico; tras 5 intentos fallidos, 429 durante 15 minutos.
* Contraseña: 8 a 72 caracteres, con al menos una letra y un número.

## 2. Catálogo

`GET /api/catalogo/productos` (Bearer) — consultado en vivo a Gestopago (copia de 30 min). Si Gestopago no responde: 503.

```json
{
  "identificador": "1-100", "idServicio": "1", "idProducto": "100",
  "nombre": "Telcel $100", "servicio": "Telcel", "descripcion": "Telcel - Telcel $100",
  "categoria": "Recargas", "logoUrl": "https://...", // puede faltar: mostrar iniciales
  "precio": 100.00, "montoVariable": false,          // si montoVariable: usar montoMinimo/montoMaximo
  "referencia": { "longitudMinima": 10, "longitudMaxima": 10, "soloDigitos": true }
}
```

Usar `referencia` para validar el formato en el formulario antes de llamar al API.

## 3. Validar referencia (obligatorio antes de pagar)

`POST /api/pagos/referencias/validar` (Bearer) `{idServicio, idProducto, referencia}`

* 200 → `{validacionId, referencia (enmascarada), precio, montoAdeudo?, expiraEn}`. Habilitar el botón Pagar.
* 422 → formato inválido o Gestopago no reconoce la referencia (mostrar `mensaje`).
* 503 → no se pudo consultar a Gestopago. **No** habilitar el pago.

El `validacionId` vence en 10 minutos y sirve para **un solo** pago.

## 4. Pagar

`POST /api/pagos/servicios` (Bearer) con encabezado `Idempotency-Key: <UUID nuevo por operación>`:

```json
{ "numeroCuenta": "0123456789", "idServicio": "1", "idProducto": "100",
  "referencia": "5512345678", "validacionId": "uuid", "monto": 100.00,
  "tarjetaGuardadaId": "tok_…", "cvv": "123" }
```

Medio de pago (obligatorio): tarjeta de débito de contado. Se envía **una** de estas opciones:
`tarjetaGuardadaId` + `cvv`, o `tarjeta: {numero, titular, mesVencimiento, anioVencimiento, cvv}` con
`guardarTarjeta: true|false`.

Mostrar primero un **resumen** (servicio, producto, referencia enmascarada, monto) y pedir confirmación. Si la petición
falla por red, reintentar con la **misma** `Idempotency-Key` (no cobra dos veces).

| HTTP | `datos.resultado` | Pantalla |
|---|---|---|
| 201 | `EXITOSO` | Éxito con `folio` y `folioProveedor`. Solo aquí se muestra éxito. |
| 422 | `RECHAZADO` | Rechazado; el monto se devolvió. |
| 202 | `PENDIENTE` | En proceso; dinero retenido. Consultar el folio más tarde. |
| 200 | (repetición) | Mismo resultado de la primera vez. |
| 409 | — | Pago duplicado o validación ya usada. |

## 4.1 Tarjetas guardadas

| Método y ruta | Uso |
|---|---|
| `GET /api/tarjetas` | Lista `[{id, marca, ultimos4, vencimiento, titular, vencida}]`. |
| `POST /api/tarjetas` | Guarda una tarjeta `{numero, titular, mesVencimiento, anioVencimiento, cvv}`. |
| `DELETE /api/tarjetas/{id}` | Elimina una tarjeta guardada. |

## 5. Historial y detalle

* `POST /api/pagos/servicios/historial` (Bearer), cuerpo opcional `{pagina: 0, tamano: 20, resultado: "EXITOSO"|"RECHAZADO"|"PENDIENTE"}`
  → `{contenido: [pagos], pagina, tamano, totalElementos, totalPaginas}`, más reciente primero. Solo pagos del usuario.
* `POST /api/pagos/servicios/consultar` (Bearer) `{folio}` → detalle (404 si no es suyo).

Cada pago trae: `folio`, `estatus`, `resultado`, `mensaje`, `servicio`, `producto`, `referencia` (enmascarada), `monto`,
`folioProveedor`, `autorizacionProveedor` (número de autorización que devuelve Gestopago; en modo simulado es un
número de 8 dígitos), `fechaOperacion`. Con tarjeta también `tarjeta` y `autorizacion` (autorización bancaria).

## Variables de entorno nuevas (Render)

| Variable | Uso |
|---|---|
| `GESTOPAGO_URL` | URL del **ambiente de pruebas** de Gestopago (sin ella se usa la URL de producción). |
| `GESTOPAGO_REFERENCIA_PATH` | Ruta del servicio de consulta de referencias de Gestopago. Sin ella, validar responde 503. |
| `GESTOPAGO_LOGO_URL` | Opcional: plantilla de logos, p. ej. `https://cdn/logos/{idServicio}.png`. |
| `APP_CORS_ORIGINS` | URL(s) del frontend web, separadas por coma. |
