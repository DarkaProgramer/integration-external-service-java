# Documento técnico · Plataforma de pago de servicios con Gesto Pago

Proyecto Hasani Pagos: una API REST propia (Spring Boot) integrada con Gesto Pago y dos interfaces que la consumen
con las mismas credenciales y las mismas funciones: una web (React + Vite) y una app móvil (Flutter + Dart).

## 1. Arquitectura

```
 ┌────────────────────┐      ┌────────────────────┐
 │  Web (React/Vite)  │      │  App (Flutter)     │
 │  Render: static    │      │  Android / iOS     │
 └─────────┬──────────┘      └─────────┬──────────┘
           │   HTTPS + JSON, Bearer JWT (mismos endpoints)
           ▼                           ▼
 ┌──────────────────────────────────────────────────┐
 │ API Hasani (Spring Boot 3, Java 17) · Render     │
 │ controller → service → repository / client       │
 │  · sesiones (JWT + refresh por dispositivo)      │
 │  · catálogo, referencias, pagos, historial       │
 └───────┬───────────────────────────┬──────────────┘
         │ JPA                        │ Feign (HTTP)
         ▼                            ▼
 ┌──────────────┐          ┌─────────────────────────┐
 │ PostgreSQL   │          │ Gesto Pago (ambiente de │
 │ (Render)     │          │ pruebas autorizado)     │
 └──────────────┘          └─────────────────────────┘
```

**Por qué las apps no llaman a Gesto Pago directamente:** Gesto Pago autentica con la contraseña del distribuidor.
Si la web o la app la llevaran, cualquiera podría sacarla del JavaScript o del APK. Por eso las dos llaman a la API
propia, que es la única que guarda las credenciales (variables de entorno) y consume Gesto Pago. Así las dos
plataformas comparten exactamente la misma lógica de negocio, que es lo que pide el punto 10.

### Capas del backend (`src/main/java/com/proyecto/servicios`)

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Presentación | `controller` | Endpoints REST, validación de entrada (`@Valid`), códigos HTTP. |
| Negocio | `service`, `service/Impl` | Reglas: sesiones, validación de referencia, pago, conciliación. |
| Integración | `client`, `client/gestopago` | Clientes Feign y adaptadores que interpretan las respuestas de Gesto Pago. |
| Persistencia | `repositorys`, `entity` | JPA sobre PostgreSQL. |
| Transversal | `config`, `seguridad`, `exception` | Tokens, CORS, manejadores de errores con respuestas seguras. |

Los adaptadores de Gesto Pago implementan interfaces (`CatalogoProductosService`, `ValidadorReferenciaGateway`,
`ProveedorPagosGateway`). Existe un modo `simulado` solo para programar sin conexión; **el predeterminado y el de la
entrega es `real`** (`GESTOPAGO_MODO=real`).

### Capas de la web (`frontend-web/src`)

`api/` (un cliente HTTP por módulo) → `auth/` (sesión) → `hooks/` (estados de carga, éxito y error) →
`components/` (reutilizables: `ProductLogo`, `PaymentResult`, `FormularioTarjeta`, `SelectorConOtro`…) → `pages/`.

### Capas de la app (`app_movil/lib`)

`core/` (red, almacenamiento seguro, utilidades) → `data/` (modelos JSON y repositorios) → `state/` (Provider) →
`ui/` (pantallas Material Design y widgets reutilizables). Las pantallas se adaptan al ancho con `ResponsiveBody`.

## 2. Servicios de Gesto Pago consumidos

| Servicio | Método y ruta | Uso en el proyecto | Configuración |
|---|---|---|---|
| Autenticación | `POST /sistema/app/jwt-gp/authenticate/` (formulario: `idDistribuidor`, `codigoDispositivo`, `password`) | Obtener el token Bearer; se renueva cada hora en segundo plano. | `GESTOPAGO_ID_DISTRIBUIDOR`, `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD` |
| Catálogo | `GET /sistema/service/getProductList.do` | Productos y servicios del catálogo (nombre, categoría, precio, reglas de referencia). | — |
| Consulta de referencia | `POST <GESTOPAGO_REFERENCIA_PATH>` | Verificar la referencia antes de pagar. | `GESTOPAGO_REFERENCIA_PATH` |
| Transacción | `POST /sistema/service/sendTx.do` | Aplicar el pago. Sin reintentos automáticos. | `GESTOPAGO_CODIGOS_APROBADA`, `GESTOPAGO_CODIGOS_RECHAZO` |
| Consulta de transacción | `POST <GESTOPAGO_CONSULTA_TX_PATH>` | Resolver pagos pendientes (conciliación cada minuto). | `GESTOPAGO_CONSULTA_TX_PATH` |

URL base: `GESTOPAGO_URL` (ambiente de pruebas autorizado; nunca producción para la entrega).

Tiempos límite: login 5 s de conexión y 10 s de lectura; catálogo 10 s; transacción 20 s. Si Gesto Pago no entrega
token, durante 60 s las peticiones responden de inmediato con error en lugar de quedarse esperando.

## 3. API propia que usan la web y la app

| Módulo | Endpoints |
|---|---|
| Sesión | `POST /api/auth/registro`, `/login`, `/refresh`, `/logout`; `GET /api/auth/perfil`, `/sesiones`; `POST /api/auth/sesiones/cerrar`; `GET /api/auth/alertas`; `POST /api/auth/alertas/leidas` |
| Catálogo | `GET /api/catalogo/productos` |
| Referencias | `POST /api/pagos/referencias/validar` |
| Pagos | `POST /api/pagos/servicios` (header `Idempotency-Key`), `POST /api/pagos/servicios/consultar` |
| Historial | `POST /api/pagos/servicios/historial` (solo pagos del usuario autenticado) |
| Tarjetas | `GET /api/tarjetas`, `POST /api/tarjetas`, `DELETE /api/tarjetas/{id}` |

Detalle de cada endpoint: [API_FRONTENDS.md](API_FRONTENDS.md) y Swagger en `/swagger-ui.html`.

Verbos HTTP: GET (consultas), POST (acciones y consultas con cuerpo), PUT (actualizar cliente), DELETE (eliminar
tarjeta). Respuesta estándar `{codigo, mensaje, timestamp, status, datos}`; errores `{status, error, mensaje, detalles}`.

## 4. Flujo de pago y estados

```
catálogo → referencia (formato local + verificación con Gesto Pago) → resumen → confirmación explícita
        → cargo a la tarjeta de débito → sendTx.do → resultado
```

| Estado que ve el usuario | Cuándo | Qué pasa con el dinero |
|---|---|---|
| **EXITOSO** | Solo si Gesto Pago respondió con un código de aprobación | Cobrado. Se muestra el folio del proveedor y el comprobante. |
| **RECHAZADO** | Gesto Pago rechazó explícitamente o la petición nunca salió | Se reembolsa a la tarjeta. |
| **PENDIENTE** | Timeout, error 5xx o respuesta que no se puede interpretar | Queda retenido; la conciliación consulta el estado y, si no se resuelve, pasa a revisión manual. |

Nunca se muestra un pago como exitoso sin la confirmación de Gesto Pago. Sin una validación vigente de la
referencia (10 minutos, un solo uso) el backend no permite pagar.

**Medio de pago:** tarjeta de débito Visa o Mastercard, de contado. El procesador de tarjetas es de práctica (no
cobra dinero real), como pide el punto 9: "No se deberán generar pagos reales sin autorización". Las respuestas de
Gesto Pago sí son las reales del ambiente de pruebas.

## 5. Seguridad

* Contraseñas de usuario con BCrypt; bloqueo de 15 min tras 5 intentos fallidos.
* Access token JWT de 15 min + refresh token rotativo por dispositivo (30 días). Web: access en memoria, refresh en
  `sessionStorage`. App: refresh en Keystore / Keychain (`flutter_secure_storage`).
* Aviso de "nuevo inicio de sesión" en los otros dispositivos con opción de cerrar esa sesión.
* Credenciales de Gesto Pago y secreto JWT solo en variables de entorno; nunca en el código. La contraseña de Gesto
  Pago viaja en el cuerpo del POST, no en la URL.
* Número de tarjeta y CVV nunca se guardan: el servidor guarda solo token, marca, últimos 4 dígitos y vencimiento.
* Errores sin trazas ni datos internos; referencias enmascaradas; campos desconocidos rechazados.
* Idempotencia en los pagos: reintentar con la misma llave nunca cobra dos veces.

## 6. Paridad web / app

| Función | Web | App |
|---|---|---|
| Registro con selectores de país, estado y municipio | ✓ | ✓ |
| Login, logout, sesiones abiertas, aviso de nuevo dispositivo | ✓ | ✓ |
| Catálogo con búsqueda, categorías y logos de proveedores | ✓ | ✓ |
| Validación de referencia (formato + Gesto Pago) | ✓ | ✓ |
| Resumen, confirmación explícita y pago con tarjeta de débito (nueva o guardada) | ✓ | ✓ |
| Resultado exitoso / rechazado / pendiente con folio | ✓ | ✓ |
| Comprobante imprimible / PDF | ✓ | ✓ |
| Historial con logo, fecha, referencia, importe, folio y estado; detalle de cada pago | ✓ | ✓ |
| Mis tarjetas (agregar y eliminar) | ✓ | ✓ |

## 7. Configuración y ejecución

Variables de entorno del backend (ver `.env.example` y `render.yaml`):

| Variable | Obligatoria | Descripción |
|---|---|---|
| `GESTOPAGO_MODO` | sí (`real`) | `real` para la entrega. |
| `GESTOPAGO_URL` | sí | URL del ambiente de pruebas de Gesto Pago. |
| `GESTOPAGO_ID_DISTRIBUIDOR`, `GESTOPAGO_CODIGO_DISPOSITIVO`, `GESTOPAGO_PASSWORD` | sí | Credenciales del distribuidor. |
| `GESTOPAGO_REFERENCIA_PATH` | sí | Ruta del servicio de consulta de referencias. |
| `GESTOPAGO_CONSULTA_TX_PATH` | recomendada | Ruta para consultar transacciones pendientes. |
| `APP_JWT_SECRET` | sí | Mínimo 32 caracteres. |
| `DATABASE_URL` | sí | PostgreSQL. |
| `APP_CORS_ORIGINS` | sí | URL de la web. |

* Backend: `./gradlew bootRun` (pruebas: `./gradlew test`).
* Web: `cd frontend-web && npm ci && npm run dev` (`VITE_API_URL`).
* App: `cd app_movil && flutter pub get && flutter run --dart-define=API_BASE_URL=<url de la API>`
  (pruebas: `flutter test`).

## 8. Pruebas automatizadas

* Backend: pruebas unitarias y de integración (JUnit 5, Mockito, H2) de sesiones, catálogo, referencias, pagos,
  conciliación y adaptadores de Gesto Pago.
* App: pruebas del cliente HTTP, repositorios, validaciones, tarjetas, comprobante PDF, ubicaciones y login.
* Pruebas funcionales con evidencia: [GUIA_EVIDENCIAS.md](GUIA_EVIDENCIAS.md).
