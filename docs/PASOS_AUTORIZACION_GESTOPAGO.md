# Autorización de Gestopago en el comprobante: pasos

Lo que pidió el profe: que el comprobante muestre el **número de autorización que devuelve Gestopago** cuando
aplica el pago. Antes solo se veía el "Folio del proveedor" y la "Autorización bancaria" (esa es del cargo a la
tarjeta, no de Gestopago).

## Qué cambió

| Parte | Cambio |
|---|---|
| Backend | `GestoPagoPagosGateway` lee el número de autorización de la respuesta de `sendTx.do` (`NUM_AUTORIZACION`, `AUTORIZACION`, `autorizacion`, etc., en XML o JSON). Se guarda en la nueva columna `autorizacion_proveedor` de `pagos_servicio` y la API lo devuelve como `autorizacionProveedor`. |
| Modo simulado | `PagosSimulados` genera un número de autorización de 8 dígitos (siempre el mismo para el mismo folio), para que se vea en la demo. |
| Web | Nueva fila **"Autorización Gestopago"** en el comprobante. Sale también en el detalle del historial y en "Imprimir o guardar comprobante (PDF)", porque usan el mismo componente. |
| App Flutter | La misma fila en la pantalla de resultado (y detalle del historial) y en el PDF del comprobante. |
| Pruebas | Pruebas nuevas en `GestoPagoPagosGatewayTest`, `ProveedorSimuladoTest`, `PagoServicioIntegrationTest` y en el test del PDF de Flutter. |

La columna nueva la crea Hibernate solo al arrancar (`ddl-auto=update`): no hay que tocar la base de datos.
Los pagos viejos no tienen el dato, así que en ellos la fila no aparece; haz un pago nuevo para verla.

## 1. Rama, commit y push (PowerShell)

```powershell
cd C:\Users\huerd\OneDrive\Desktop\Proyecto-Hassani\prueba
git checkout -b feature/autorizacion-gestopago
git add src app_movil/lib app_movil/test frontend-web/src docs
git commit -m "feat(pagos): mostrar el número de autorización de Gestopago en el comprobante"
git push -u origin feature/autorizacion-gestopago
```

## 2. Llevarla a main

En GitHub abre un Pull Request de `feature/autorizacion-gestopago` a `main` y haz **Merge**. Render redespliega
`hasani-api` y `hasani-web` solos (5 a 10 minutos).

## 3. Comprobar

1. Entra a hasani-web, paga cualquier servicio (por ejemplo Telcel $20).
2. En el comprobante debe salir **Autorización Gestopago** con un número, debajo de "Folio del proveedor".
3. Abre **Historial**, entra al pago: también aparece. Con **Imprimir o guardar comprobante (PDF)** sale en el PDF.
4. En la app: `flutter run`, haz un pago y revisa la pantalla de resultado y el botón del PDF.

## Para explicarle al profe

Cuando Gestopago aplica el pago responde con un código de éxito y un número de autorización. La API lo toma de
esa respuesta, lo guarda junto al pago y lo regresa en el comprobante. Es el número con el que se puede
reclamar o rastrear la operación ante Gestopago. En modo simulado (sin conexión real) se genera uno de 8
dígitos para la demostración.
