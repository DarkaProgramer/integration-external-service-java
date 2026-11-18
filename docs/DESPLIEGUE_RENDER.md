# Despliegue en Render

La aplicación corre en Render como servicio web Docker (`Dockerfile`) con una base PostgreSQL de Render.
Todo se crea con el Blueprint `render.yaml`.

## Qué cambió respecto a Railway

| Tema | Railway | Render |
|---|---|---|
| Base de datos | MySQL (`MYSQL_URL`) | PostgreSQL (`DATABASE_URL`, la inyecta el Blueprint) |
| Build | Nixpacks | `Dockerfile` (Render no tiene runtime nativo de Java) |
| Puerto | `PORT` | `PORT` (Render usa 10000), ya lo lee `server.port=${PORT:8081}` |
| Health check | — | `/actuator/health` |

* `ConfigDB` acepta `postgresql://usuario:clave@host/db` (Render), `mysql://...` (Railway) o una URL `jdbc:`
  y separa usuario y contraseña (`UrlBaseDatos`). El dialecto de Hibernate se detecta solo, así que el mismo
  build funciona local con MySQL y en Render con PostgreSQL.
* Las tablas las crea Hibernate al arrancar (`ddl-auto=update`). Los datos de Railway no se copian.
* Probado contra PostgreSQL 16: arranca, crea las 7 tablas, registra clientes y el flujo de pagos responde.

## Pasos

1. Subir los cambios a GitHub y llevarlos a `main` (el Blueprint despliega `main`):
   ```bash
   git push origin desarrollador
   git checkout main && git merge --no-ff desarrollador && git push origin main
   ```
2. En Render: **New → Blueprint**, conectar el repositorio `integration-external-service-java` y aceptar.
   Render crea `hasani-db` (PostgreSQL) y `hasani-api` (web).
3. Render pide los valores marcados `sync: false`:
   * `GESTOPAGO_PASSWORD`: contraseña de Gestopago (idealmente una nueva, porque la anterior estuvo en el repo).
   * `GESTOPAGO_PRODUCTS_TOKEN` y `GESTOPAGO_TX_TOKEN`: opcionales. Con `GESTOPAGO_PASSWORD` la API pide el token
     a Gestopago sola y lo renueva cada hora (`GestoPagoBearerToken`); estos tokens fijos solo se usan si no hay uno
     renovado, y caducan.
   `APP_JWT_SECRET` lo genera Render solo y `DATABASE_URL` sale de la base.
4. Esperar el primer deploy (5 a 10 minutos) y abrir `https://hasani-api.onrender.com/actuator/health` →
   `{"status":"UP"}`. Swagger: `/swagger-ui.html`.
5. Cuando funcione, apagar el servicio de Railway.

## Notas del plan gratuito

* El servicio se duerme tras 15 minutos sin tráfico; la primera petición tarda ~1 minuto en despertar.
  Mientras duerme, el job de conciliación no corre (los pagos dudosos esperan con el dinero retenido).
* La base PostgreSQL gratis **caduca a los 30 días**; para la entrega alcanza, después hay que pasarla a un plan
  de pago o recrearla.
* El endpoint `/api/dev/token` no existe en Render (solo con el perfil `dev`). Para una demostración en
  Render se puede agregar temporalmente la variable `SPRING_PROFILES_ACTIVE=dev`, y quitarla después.

## Si el catálogo dice "No fue posible validar el producto"

La API no pudo descargar `getProductList.do` de Gestopago. En **Render → hasani-api → Logs** busca
`No fue posible obtener el catálogo de GestoPago` o `Error al renovar token GestoPago`; el campo `detalle`
dice qué revisar:

| Detalle en el log | Qué hacer |
|---|---|
| `tipo=AUTHENTICATION (falta GESTOPAGO_PASSWORD ...)` | Agregar `GESTOPAGO_PASSWORD` en Environment |
| `HTTP 401` / `HTTP 403` o `Error al renovar token ... HTTP 401` | La contraseña, `GESTOPAGO_ID_DISTRIBUIDOR` o `GESTOPAGO_CODIGO_DISPOSITIVO` no corresponden al ambiente de `GESTOPAGO_URL` |
| `HTTP 404` | `GESTOPAGO_URL` mal escrita: debe ser solo la URL base, sin `/sistema/...` |
| `sin conexión o timeout` | `GESTOPAGO_URL` no responde desde Render |
| `sin productos reconocibles` | Gestopago respondió, pero con un error o un formato distinto |
