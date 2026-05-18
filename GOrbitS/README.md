# GOrbitS — demo rápida

Backend **Spring Boot** para el MVP (catálogo, guías de venta, inventario, cuotas y resumen admin).

## Arranque local (Mac — H2, sin MySQL)

Por defecto: perfil **`h2`**, base en archivo `./data/gorbits`, datos demo con seeds Java.

```bash
cd GOrbitS
./mvnw spring-boot:run
```

Usuarios: `admin` / `admin123`, `proveedor` / `proveedor123`, embajadores `embajador01`… / `embajador123` (tras el primer arranque completo).

Si algo quedó corrupto en H2: borrá `GOrbitS/data/` y volvé a arrancar.

Ver SQL en consola: `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2,dev`

## Arranque como el servidor (MySQL/MariaDB)

Para probar contra la misma BD que el JAR en producción:

1. Creá/importá la base (`database/migrations/` o `db/mysql/gorbits_seed.sql`).
2. Levantá con perfil **`mysql`**:

```bash
cd GOrbitS
MYSQL_USER=tuusuario MYSQL_PASSWORD=tuclave ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

JAR en producción:  
`java -jar target/GOrbitS-*.jar --spring.profiles.active=mysql,prod`

Puerto por defecto: **8080** (`http://localhost:8080`).

### Entrega a servidor (separación de roles)

**Backend developer** entrega (ver `ENTREGA_BACKEND.md`):

- JAR compilado
- `database/migrations/*.sql`
- `database/env.required.example`
- Notas de versión

Compilar: `./mvnw package -DskipTests` → entregar JAR + `database/migrations/` + `ENTREGA_BACKEND.md`.

**DevOps / servidor** define: `deploy.sh`, Nginx, estructura en disco, backups y roles de BD.

Requisitos de runtime del API: puerto **8080**, prefijo **`/api/v1`**, perfiles `mysql,prod`. Frontend aparte (`GOrbitSF`, `apiBaseUrl: '/api/v1'`).

Perfil **`h2`** (por defecto en Mac): H2 en `./data/gorbits`. Perfil **`mysql`**: MariaDB/MySQL como en servidor. Perfil **`test`**: H2 en memoria para tests.

## Usarios de prueba (seed)

| Usuario    | Contraseña    | Rol        |
|------------|----------------|------------|
| `admin`    | `admin123`     | ADMIN      |
| `proveedor`| `proveedor123` | PROVEEDOR  |

## URLs útiles en reuniones

| Qué | URL |
|-----|-----|
| **Proveedores (id + login)** solo admin | `GET /api/v1/admin/providers` |
| **Swagger UI** (listar y probar endpoints) | http://localhost:8080/swagger-ui |
| Especificación OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Login (obtener JWT) | `POST /api/v1/auth/login` |
| Salud del servicio | http://localhost:8080/actuator/health |
| Info corta | http://localhost:8080/actuator/info |

En Swagger: **Authorize** → pega solo el `accessToken` (sin la palabra `Bearer`). El id de usuario proveedor sale de `GET /api/v1/admin/providers` (solo admin) o de `GET /api/v1/me` si entras como proveedor.

## Rol proveedor (inventario + librería)

| Uso | Método y ruta |
|-----|----------------|
| Ver stock en almacén (solo lectura) | `GET /api/v1/inventory/warehouse` |
| Stock en campo | `GET /api/v1/inventory/field` |
| Retiros al campo | `POST /api/v1/inventory/withdrawals` |
| **Facturas de compra a la librería** (cada línea: `quantity` + `lineTotal`; el proveedor de la factura es el usuario **proveedor** actual o el **`ownerUserId`** que indique el admin; número de factura único **por proveedor**) | `GET/POST /api/v1/inventory/library-invoices` — **admin**: `GET ?providerId=` opcional; **POST** con **`ownerUserId`** obligatorio. **Proveedor**: solo ve las suyas; **POST** sin `ownerUserId` |
| **Depósitos / pagos a la librería** | `GET/POST /api/v1/inventory/library-payments` (opcional `campaignId`) |
| **Devolución física de stock a la librería** (sale del almacén) | `POST /api/v1/inventory/library-stock-returns` (opcional `campaignId` para cuadrar por campaña) |
| **Conciliación** (unidades + importes facturados vs depósitos → saldo) | `GET /api/v1/inventory/library-reconciliation/summary?campaignId=` |

Sin `campaignId`, los importes de factura y unidades compradas a la librería son **los del proveedor** (no del sistema entero); los depósitos **del mismo proveedor**. Con `campaignId`, facturas por fecha de emisión dentro del periodo de campaña (solo de ese proveedor) y depósitos **etiquetados** con esa campaña. `netBalanceOwedToLibrary` = facturado − depósitos (positivo = aún debes a la librería en dinero).

Ajustar cantidades en almacén a mano sigue siendo solo **admin** (`PUT /api/v1/inventory/warehouse/books/{bookId}`).

## Flujo de demo (5 minutos)

1. **Login** como `proveedor` → JWT.
2. **Catálogo** → `GET /api/v1/catalog/books`.
3. **Comercial** → clientes, campañas (`/api/v1/reference/...`), crear guía (`/api/v1/guides`).
4. **Inventario** → ver almacén; retiros al campo; factura de librería; si aplica devolución a librería; **conciliación** por campaña.
5. **Admin** → `GET /api/v1/admin/dashboard` — `GET /api/v1/admin/providers` — **gestión de usuarios** (listado, **nueva contraseña** si alguien olvidó la suya, alta de proveedor), ver tabla siguiente.

Más detalle en cada tag del Swagger.

## Admin: usuarios (solo `ADMIN`)

No hay recuperación automática por email en el MVP: si un usuario **olvida la contraseña**, debe contactar al administrador; este le asigna una nueva con `PUT .../password`.

| Acción | Método y ruta |
|--------|----------------|
| Listar cuentas (`id`, `username`, `role`, `enabled`) | `GET /api/v1/admin/users` |
| **Asignar contraseña nueva** a un usuario | `PUT /api/v1/admin/users/{id}/password` cuerpo: `{"newPassword":"..."}` (mín. 8 caracteres) |
| Activar / desactivar cuenta (no puede deshabilitarse a sí mismo) | `PATCH /api/v1/admin/users/{id}/enabled` cuerpo: `{"enabled":true}` |
| Crear proveedor (y perfil con la primera zona del sistema) | `POST /api/v1/admin/users/providers` cuerpo: `{"username":"...","password":"..."}` |

## Producción

Para no exponer la documentación: `springdoc.swagger-ui.enabled=false` y `springdoc.api-docs.enabled=false` (por ejemplo en el perfil de despliegue).
