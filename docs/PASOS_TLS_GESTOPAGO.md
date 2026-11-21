# Certificado de Gestopago y versión TLS: pasos

Esto es lo que pidió el profe: importar el certificado de Gestopago al truststore de Java, activar SSL desde
las propiedades y fijar la versión de TLS en el cliente. Ya está programado (`GestoPagoSslConfig.java`).
Se activa con variables de entorno, así que no hay que tocar más código.

## 1. Mira primero el error real en Render

Render → **hasani-api** → **Logs**. Busca `No fue posible obtener el catálogo de GestoPago` o
`Error al renovar token GestoPago`. Lo que dice `causa=` y `detalle=` te indica el problema:

| Lo que ves | Qué significa |
|---|---|
| `SSLHandshakeException`, `PKIX path building failed`, `unable to find valid certification path` | Falta el certificado: haz los pasos 2 a 4 |
| `handshake_failure` o `protocol_version` | La versión de TLS no coincide: prueba `GESTOPAGO_TLS_VERSION` = `TLSv1.2` (o `TLSv1.3`) |
| `HTTP 401` o `HTTP 403` | Contraseña o usuario incorrecto, o acceso bloqueado: pídelo al profe o a Gestopago |
| `ConnectException`, `UnknownHost`, `SocketTimeout` o `connect timed out` | No hay conexión con su servidor (por ejemplo, una IP bloqueada): el certificado no lo arregla |

## 2. Descargar el certificado de Gestopago (en tu compu)

Si el profe ya te dio el archivo `.crt` o `.cer`, sáltate este paso. Si no, en PowerShell (Git trae `openssl`):

```powershell
cd C:\Users\huerd\OneDrive\Desktop\Proyecto-Hassani\prueba
"" | & "C:\Program Files\Git\usr\bin\openssl.exe" s_client -connect gestopago.portalventas.net:443 -servername gestopago.portalventas.net -showcerts > gestopago-cadena.txt
```

Abre `gestopago-cadena.txt` y copia **desde** `-----BEGIN CERTIFICATE-----` **hasta** `-----END CERTIFICATE-----`
(todos los bloques que aparezcan) a un archivo `gestopago.crt`.

Otra forma: abre `https://gestopago.portalventas.net` en Chrome, haz clic en el candado →
**La conexión es segura** → **El certificado es válido** → **Detalles** → **Exportar** (formato Base64 `.crt`).

(Opcional, como lo dijo el profe, con keytool: `keytool -importcert -alias gestopago -file gestopago.crt -keystore gestopago.p12 -storetype PKCS12 -storepass changeit -noprompt`.)

## 3. Subirlo a Render (sin meterlo a GitHub)

Render → **hasani-api** → **Environment** → **Secret Files** → **Add Secret File**:
* Filename: `gestopago.crt`
* Contents: pega el contenido del archivo (los bloques BEGIN/END).

Render lo pone en `/etc/secrets/gestopago.crt`. Si usaste keytool, sube `gestopago.p12` igual y usa
`GESTOPAGO_TRUSTSTORE` (ver abajo).

## 4. Variables de entorno en Render (hasani-api → Environment)

| Variable | Valor |
|---|---|
| `GESTOPAGO_SSL_ENABLED` | `true` |
| `GESTOPAGO_TLS_VERSION` | `TLSv1.2` (o la que te diga el profe; varias separadas por coma: `TLSv1.2,TLSv1.3`) |
| `GESTOPAGO_CERT_PATH` | `/etc/secrets/gestopago.crt` |
| `GESTOPAGO_TRUSTSTORE` | solo si usaste keytool: `/etc/secrets/gestopago.p12` (y `GESTOPAGO_TRUSTSTORE_PASSWORD`) |
| `GESTOPAGO_MODO` | `real` |

**Save Changes** → **Manual Deploy**. En los logs del arranque debe salir
`TLS de GestoPago activo: protocolos=TLSv1.2 certificadosDeConfianza=...`. Si el archivo no existe o no es un
certificado válido, la API no arranca y el log dice por qué.

## 5. Subir el código

```powershell
cd C:\Users\huerd\OneDrive\Desktop\Proyecto-Hassani\prueba
git checkout -b feature/tls-gestopago
git add src/main/java/com/proyecto/servicios/config/GestoPagoSslConfig.java src/main/resources/application.properties src/test
git commit -m "feat(gestopago): certificado en el truststore y versión TLS configurable"
git push -u origin feature/tls-gestopago
git checkout main
git merge feature/tls-gestopago
git push origin main
```

Revisa en GitHub que `main` muestre ese commit **antes** de desplegar en Render.

## Plan B para la presentación

Si aun así Gestopago no responde (por ejemplo, por la IP bloqueada), pon `GESTOPAGO_MODO` = `simulado` y
redespliega. El catálogo y los pagos funcionan, y el comprobante muestra "Autorización Gestopago" con un
número simulado.
