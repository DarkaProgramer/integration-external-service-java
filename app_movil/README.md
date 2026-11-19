# Hasani Pagos · App móvil (Flutter)

App Android/iOS del proyecto de pago de servicios con **Gestopago**. Consume la **misma API REST** que la web
(backend Spring Boot de este repo), con las mismas credenciales: un usuario puede entrar desde la web y desde el
teléfono a la vez.

## Requisitos

* Flutter 3.27 o más reciente (`flutter --version`).
* Android Studio con un emulador o un teléfono con depuración USB.

## Ejecutar

```bash
cd app_movil
flutter pub get

# Contra el backend en Render
flutter run --dart-define=API_BASE_URL=https://hasani-api.onrender.com

# Contra el backend local (emulador de Android → tu PC es 10.0.2.2)
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8081
```

Sin `--dart-define` se usa `https://hasani-api.onrender.com`. El plan gratis de Render se duerme: la primera
petición puede tardar ~50 s (la app espera hasta 60 s).

Pruebas y análisis:

```bash
flutter analyze
flutter test
```

## Funciones (iguales a la web)

| Módulo | Pantalla | Endpoints |
|---|---|---|
| Sesión | Login, registro, cerrar sesión, dispositivos abiertos | `POST /api/auth/login`, `/registro`, `/refresh`, `/logout`, `GET /perfil`, `GET /sesiones`, `POST /sesiones/cerrar` |
| Aviso de nuevo dispositivo | Diálogo "Nuevo inicio de sesión" con "No fui yo, cerrar esa sesión" | `GET /api/auth/alertas` cada 20 s, `POST /api/auth/alertas/leidas` |
| Catálogo | Tarjetas con logo (los mismos de la web, en `assets/logos`), nombre, proveedor, categoría, descripción, precio; búsqueda y filtro por categoría | `GET /api/catalogo/productos` |
| Validar referencia | Formato según el producto + verificación en Gestopago. El botón de pagar está bloqueado hasta validar | `POST /api/pagos/referencias/validar` |
| Pago | Resumen, tarjeta de débito (guardada + CVV o nueva, con vista previa), confirmación explícita, resultado EXITOSO / RECHAZADO / PENDIENTE con folio y comprobante PDF | `POST /api/pagos/servicios` (+ `Idempotency-Key`) |
| Mis tarjetas | Agregar y eliminar tarjetas de débito (pantalla de cuenta) | `GET`, `POST /api/tarjetas`, `DELETE /api/tarjetas/{id}` |
| Registro | Selectores de país, estado y municipio con opción "Otro (escribir)" (mismo catálogo que la web, en `assets/data`) | `POST /api/auth/registro` |
| Historial | Lista paginada con logo, fecha, referencia, importe, folio y estado; filtro por estado; detalle | `POST /api/pagos/servicios/historial`, `/consultar` |

Reglas importantes:

* **Nunca** se muestra un pago como exitoso sin que el API devuelva `resultado: EXITOSO`. Cualquier otro valor se
  muestra como pendiente.
* Si la red falla al pagar, se reintenta con la **misma** `Idempotency-Key`, así que no se cobra dos veces.
* Si Gestopago no responde al validar (HTTP 503), la app muestra el error y **no** deja continuar.

## Arquitectura (por capas)

```
lib/
├── main.dart                 # arma las dependencias (Provider)
├── app.dart                  # tema Material 3 y puerta de sesión
├── core/                     # infraestructura sin lógica de negocio
│   ├── config/app_config.dart      # API_BASE_URL por --dart-define, tiempos
│   ├── network/api_client.dart     # HTTP, Bearer, envoltura {datos}, errores, refresh ante 401
│   ├── network/api_exception.dart
│   ├── storage/token_storage.dart  # refreshToken en Keystore/Keychain (flutter_secure_storage)
│   └── utils/                      # validaciones, formatos, JSON, nombre del dispositivo
├── data/
│   ├── models/               # Perfil, SesionTokens, Producto, ValidacionReferencia, Pago…
│   └── repositories/         # Auth, Catalogo, Pagos, Tarjetas y Ubicaciones
├── state/                    # ChangeNotifier: sesión, alertas, catálogo, historial, navegación
└── ui/
    ├── screens/              # login, registro, catálogo, referencia, confirmar, resultado, historial, cuenta
    └── widgets/              # componentes reutilizables (logo, tarjetas, estados de carga/error…)
```

* **Seguridad**: el accessToken (15 min) solo vive en memoria; el refreshToken se guarda cifrado con
  `flutter_secure_storage`. No hay contraseñas, llaves ni URLs secretas en el código. Android solo permite HTTPS
  (salvo `10.0.2.2`/`localhost` para desarrollo) y tiene `allowBackup=false`.
* **Diseño adaptable**: barra de navegación inferior en teléfonos y riel lateral en tabletas u horizontal; el
  contenido se limita de ancho en pantallas grandes y la cuadrícula del catálogo ajusta sus columnas.

## Publicar en Google Play

* `applicationId`: `com.hasani.pagos` (iOS: `com.hasani.pagos`). No se puede cambiar después de publicar.
* Versión: `version: 1.0.0+1` en `pubspec.yaml` (súbele el `+N` en cada subida).

1. Crea la llave de subida (una sola vez, guárdala bien y **no** la subas a git):
   ```bash
   keytool -genkey -v -keystore C:/Users/TU_USUARIO/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Copia `android/key.properties.example` como `android/key.properties` y llena tus datos.
3. Genera el paquete:
   ```bash
   flutter build appbundle --release --dart-define=API_BASE_URL=https://hasani-api.onrender.com
   ```
   El archivo queda en `build/app/outputs/bundle/release/app-release.aab`. Súbelo a Play Console, primero en
   **Prueba interna**.

Sin `key.properties`, el release se firma con la llave de debug: sirve para instalar el APK en un teléfono
(`flutter build apk --release`), pero Play Console no lo acepta.

## Si el proyecto Android no compila con tu versión de Flutter

Las carpetas `android/` e `ios/` se generaron con Flutter 3.50. Si tu Flutter es más viejo y Gradle falla, regenera
solo lo que falte sin tocar `lib/`:

```bash
cd app_movil
flutter create --org com.hasani --project-name hasani_pagos --platforms android,ios .
```

y vuelve a aplicar en `android/app/build.gradle(.kts)` el `applicationId`, `minSdk = 23` y la firma de release.
