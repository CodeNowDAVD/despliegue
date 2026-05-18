# Postman — Sesión 07 Pruebas de Integración (GOrbitS)

Colección para el curso **Electivo I – Testing del Software**, alineada con la guía de Canvas (HTTP, JWT, códigos 200/401/403/404/409, flujos de negocio).

## Archivos

| Archivo | Uso |
|---------|-----|
| `GOrbitS-Sesion07-Integracion.postman_collection.json` | Colección con tests en cada request |
| `GOrbitS-Dev-H2.postman_environment.json` | Backend local con H2 (`dev`) |
| `GOrbitS-Prod-MariaDB.postman_environment.json` | Mismo `baseUrl`; cambiar si el servidor usa otro host/puerto |

## Postman en el navegador vs local (`localhost`)

Si ves **"Cloud agent error: cannot send request"** al pegarle a `http://localhost:8080`:

- El Postman **web** no llega a tu máquina solo; hace falta el **Postman Desktop Agent**.
- En **Safari** el agente de escritorio **no funciona** → usa **Chrome o Firefox** + [Desktop Agent](https://www.postman.com/downloads/postman-agent/), **o** la app **Postman Desktop** (recomendado para esta práctica).

Sin eso, el login nunca responde y `jwt` queda vacío.

## Importar en Postman

1. **Import** → arrastrar los 3 archivos JSON.
2. Seleccionar entorno **GOrbitS Dev (H2)** (esquina superior derecha, **obligatorio**).
3. Levantar el backend (ver abajo).
4. Colección → **Run** (Runner) → ejecutar carpetas en orden `0` → `5`.

## ¿Dónde va el `accessToken`? (JWT)

**No lo pegas a mano en cada request** si sigues el flujo de la colección.

### Automático (recomendado)

1. Con entorno **GOrbitS Dev (H2)** activo, ejecuta **`0 — Salud y autenticación` → `POST Login proveedor`**.
2. En la respuesta verás JSON con `accessToken`.
3. En la pestaña **Tests** de ese request hay un script que hace:
   - `pm.environment.set('jwt', accessToken)`
4. El resto de requests ya llevan el header:
   - `Authorization: Bearer {{jwt}}`

Comprueba: icono del ojo 👁 → **Environment** → variable `jwt` debe tener un valor largo después del login.

En **Collection Runner**, la carpeta **0** debe ir **primera** (ahí se obtiene el token).

### Manual (solo si falla el script)

1. Ejecuta **Login proveedor** y copia el valor de `accessToken` del body (sin comillas).
2. Entorno **GOrbitS Dev (H2)** → **Edit** → variable `jwt` → pega el token → **Save**.
3. O en un request: pestaña **Authorization** → Type **Bearer Token** → Token = `{{jwt}}`.

No uses la pestaña Authorization de la colección entera con un token fijo: caduca; el flujo correcto es `{{jwt}}` + login en carpeta 0.

## Levantar el backend

### Dev (H2, seeds demo)

```bash
cd GOrbitS
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2,dev
```

Importante: el perfil **`h2`** activa los usuarios demo (`proveedor` / `proveedor123`). Con solo `dev` el login fallará (401).

O con el JAR:

```bash
java -jar GOrbitS-0.0.1-SNAPSHOT.jar --spring.profiles.active=h2,dev
```

### Prod / MariaDB (comparación sesión)

```bash
export SPRING_PROFILES_ACTIVE=mysql,prod
export JWT_SECRET='...min 32 chars...'
export MYSQL_URL='jdbc:mariadb://localhost:3306/gorbits'
export MYSQL_USER=gorbits
export MYSQL_PASSWORD=...
java -jar GOrbitS-0.0.1-SNAPSHOT.jar
```

Aplicar migraciones en `database/migrations/` antes del primer arranque.

Credenciales demo: ver `ENTREGA_BACKEND.md` (`proveedor` / `proveedor123`, `admin` / `admin123`).

## Qué valida la colección

| Carpeta | Contenido |
|---------|-----------|
| **0** | Health, login JWT, `/me`, 401 sin token |
| **1** | Campañas, catálogo, factura de stock |
| **2** | Cliente, crear guía, GET, 404 (**no** cerrar guía aquí; ver nota abajo) |
| **3** | Plan de cuotas, calendario, pago, resumen billing |
| **4** | `GuideLifecycleRules`: no marcar DEVUELTA por PATCH; devolución; pago bloqueado 409 |
| **5** | Admin sin acceso a `/clients` → 403 |

Variables que se guardan solas: `jwt`, `campaignId`, `bookId`, `clientId`, `guideId`, `installmentId`.

**Si ves `installments//payments` (400):** saltaste el guardado de `installmentId`. Suele pasar si ejecutaste **`PATCH → CERRADA`** (carpeta 2) antes de **`POST Plan cuotas`** (carpeta 3). Vuelve a crear guía o omite el PATCH hasta después del pago.

## Evidencia para el informe / evaluación

1. Captura del **Collection Runner** con todos los tests en verde.
2. Captura de **Login** mostrando `accessToken` en el body.
3. Captura de un request con pestaña **Tests** (assertions).
4. Tabla breve **H2 vs MariaDB**: mismo flujo en ambos entornos; anotar si IDs o totales difieren (datos seed).

## Newman (opcional, línea de comandos)

Con el servidor en marcha:

```bash
npx newman run postman/GOrbitS-Sesion07-Integracion.postman_collection.json \
  -e postman/GOrbitS-Dev-H2.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export postman/newman-report.html
```

## Relación con tests automatizados

Los flujos replican `CommercialApiTest`, `BillingApiTest`, `AuthApiTest` y `GuideReturnApiTest` en `src/test/java/`. Postman es la capa manual de integración de la Sesión 07; MockMvc sigue siendo la suite CI en `./mvnw test`.
