# Hasani Pagos — frontend web

Aplicación web (React 18 + Vite) para pagar servicios con Gestopago a través del backend Spring Boot de este repo
(`/api/auth`, `/api/catalogo`, `/api/pagos`). Tiene las mismas funciones que la app Flutter.

## Funciones

| Pantalla | Ruta | Endpoints |
|---|---|---|
| Iniciar sesión / Registro (3 pasos) | `/login`, `/registro` | `POST /api/auth/login`, `/registro` |
| Catálogo con logos, categorías y buscador | `/` | `GET /api/catalogo/productos` |
| Validar referencia → resumen → comprobante | `/pagar/:identificador` | `POST /api/pagos/referencias/validar`, `POST /api/pagos/servicios` |
| Historial paginado con filtro por resultado | `/historial` | `POST /api/pagos/servicios/historial` |
| Detalle / comprobante | `/historial/:folio` | `POST /api/pagos/servicios/consultar` |
| Mi cuenta: perfil y sesiones abiertas | `/cuenta` | `GET /api/auth/perfil`, `/sesiones`, `POST /sesiones/cerrar` |
| Aviso de inicio de sesión en otro dispositivo | (modal global) | `GET /api/auth/alertas` cada 20 s, `POST /alertas/leidas` |

Reglas que respeta:

* **No se puede pagar sin validar la referencia.** Si el backend responde 503 (Gestopago no disponible o la ruta de
  referencias sin configurar) se muestra el error y el botón de pagar no aparece. Nunca se simula un éxito.
* El pago lleva un `Idempotency-Key` (UUID) por operación. Si se cae la red, se reintenta con la **misma** llave.
* Solo un 201 `EXITOSO` muestra éxito; 422 `RECHAZADO` y 202 `PENDIENTE` tienen su propia pantalla, con "Consultar estado".
* Formularios validados con las mismas reglas que el backend (CURP, RFC, teléfono, CP, contraseña, formato de la
  referencia según el catálogo).

## Estructura (por capas)

```
src/
  config/env.js           URL del API desde VITE_API_URL
  api/                    cliente HTTP (http.js: errores, refresh automático en 401) y un módulo por recurso
  auth/                   tokenStore (access en memoria, refresh en sessionStorage) y AuthContext
  hooks/                  useAsync (carga/error/recarga), useAlertasSesion (polling de alertas)
  components/             piezas reutilizables: Button, Field, Alert, Modal, ProductLogo, StatusBadge, ...
  pages/                  una pantalla por archivo
  utils/                  formato de moneda/fecha, validaciones, datos del dispositivo
  styles/global.css       diseño responsivo con modo claro/oscuro
```

Seguridad de la sesión: el `accessToken` (15 min) vive solo en memoria y el `refreshToken` en `sessionStorage`
(se borra al cerrar la pestaña). En cualquier 401 se intenta `POST /api/auth/refresh` una sola vez; si falla, se manda
al login. No hay secretos en el código: lo único configurable es la URL pública del API.

## Correr en local

```bash
cd frontend-web
cp .env.example .env.local      # ajusta VITE_API_URL si tu backend no está en http://localhost:8081
npm install
npm run dev                     # http://localhost:5173 (ya está permitido en APP_CORS_ORIGINS por defecto)
```

## Desplegar en Render (sitio estático)

El `render.yaml` de la raíz ya incluye el servicio `hasani-web`. Si lo creas a mano:

* **New → Static Site**, mismo repo, **Root Directory**: `frontend-web`
* **Build Command**: `npm ci && npm run build` · **Publish Directory**: `dist`
* **Environment**: `VITE_API_URL=https://hasani-api.onrender.com`
* **Redirects/Rewrites**: Source `/*` → Destination `/index.html` → Action **Rewrite** (para que funcionen las rutas al recargar)
* En el backend (`hasani-api`) agrega la URL del sitio a `APP_CORS_ORIGINS`, p. ej. `https://hasani-web.onrender.com`.
