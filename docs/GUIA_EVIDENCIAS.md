# Guía de evidencias y pruebas funcionales

Lista para grabar el video y tomar las capturas que pide el profesor (puntos 8 y 9). Guarda todo en la carpeta
`evidencias/` con los nombres sugeridos.

## Antes de empezar

1. En Render, la API debe tener `GESTOPAGO_MODO=real` y todas las variables de Gesto Pago (ver
   [DOCUMENTO_TECNICO.md](DOCUMENTO_TECNICO.md#7-configuración-y-ejecución)).
2. Abre `https://<tu-api>/actuator/health` para despertar el servidor (el plan gratis tarda ~50 s).
3. En los logs de Render debe aparecer `Token GestoPago renovado correctamente`. Si aparece
   `Error al renovar token GestoPago`, revisa la URL y las credenciales antes de grabar.
4. Ten a la mano una referencia válida y una incorrecta del ambiente de pruebas de Gesto Pago (pídelas al profesor).

## Evidencia del consumo de Gesto Pago

| # | Qué capturar | Archivo |
|---|---|---|
| E1 | Logs de Render con `Token GestoPago renovado correctamente` | `evidencias/01-token-gestopago.png` |
| E2 | Logs con `GestoPago sendTx folio=… estado=APROBADA` después de un pago | `evidencias/02-sendtx.png` |
| E3 | Swagger (`/swagger-ui.html`) con `GET /api/catalogo/productos` respondiendo datos de Gesto Pago | `evidencias/03-catalogo-api.png` |

## Pruebas funcionales obligatorias

| # | Escenario | Web | App | Resultado esperado |
|---|---|---|---|---|
| P1 | Inicio de sesión desde la web | `10-login-web.png` | — | Entra al catálogo. |
| P2 | Inicio de sesión desde la app con el mismo usuario | — | `11-login-app.png` | Entra; la web muestra el aviso "Nuevo inicio de sesión". |
| P3 | Consulta del catálogo | `12-catalogo-web.png` | `13-catalogo-app.png` | Productos de Gesto Pago con nombre, categoría y descripción. |
| P4 | Logotipos de los proveedores | (mismas capturas de P3) | (mismas) | Cada proveedor con su logo. |
| P5 | Referencia correcta | `14-ref-ok-web.png` | `15-ref-ok-app.png` | "Referencia válida" con los datos que devuelve el servicio. |
| P6 | Referencia incorrecta | `16-ref-mal-web.png` | `17-ref-mal-app.png` | Mensaje de error y no deja continuar al pago. |
| P7 | Confirmación de un pago | `18-confirmar-web.png` | `19-confirmar-app.png` | Resumen y diálogo de confirmación explícita. |
| P8 | Resultado de la transacción | `20-resultado-web.png` | `21-resultado-app.png` | Exitoso con folio (o rechazado / pendiente, según responda Gesto Pago). |
| P9 | Historial de pagos | `22-historial-web.png` | `23-historial-app.png` | Logo, servicio, fecha, referencia, importe, folio y estado. |
| P10 | Operaciones desde ambas plataformas | `24-detalle-web.png` | `25-detalle-app.png` | El pago hecho en la app aparece en la web y viceversa. |

Tarjeta de práctica para los pagos (no cobra dinero real): `4000 1234 5675 9010`, cualquier titular, fecha futura y
cualquier CVV de 3 dígitos. Para un rechazo del banco: `4000 1234 5674 0002`.

## Guion del video (5 a 8 minutos)

1. Arquitectura en 30 s (diagrama del documento técnico).
2. Web: login → catálogo con logos → referencia incorrecta (rechazo) → referencia correcta → confirmar → resultado
   con folio → historial → detalle.
3. App: login con el mismo usuario (mostrar el aviso en la web) → catálogo → pago → resultado → historial, donde
   también aparece el pago hecho en la web.
4. Cerrar sesión en las dos.
5. Logs de Render mostrando las llamadas a Gesto Pago.
