# Notas de versión — GOrbitS (entrega backend)

Documento de **handoff del desarrollador backend** a DevOps/servidor.  
Los scripts de despliegue (`deploy.sh`, Nginx, estructura `~/servers/gorbits/`, etc.) los define y mantiene el equipo de servidor.

---

## 1. Artefacto compilado

| Archivo | Descripción |
|---------|-------------|
| `GOrbitS-0.0.1-SNAPSHOT.jar` | JAR Spring Boot (API + actuator; sin servir SPA en perfil `prod`) |

Generar y entregar al servidor:

```bash
cd GOrbitS && ./mvnw package -DskipTests
# → target/GOrbitS-0.0.1-SNAPSHOT.jar
```

**Contenido del paquete** (carpeta o zip que suba DevOps):

- `GOrbitS-0.0.1-SNAPSHOT.jar`
- `database/migrations/` (todos los `V*.sql`)
- `database/env.required.example`
- `ENTREGA_BACKEND.md` (este archivo)

---

## 2. Migraciones SQL

Carpeta: `database/migrations/`

| Orden | Archivo | Qué hace |
|-------|---------|----------|
| 1 | `V20260517_1200__esquema_inicial_gorbits.sql` | **Crea tablas** (IF NOT EXISTS), FK, índices; tabla `schema_migrations` |
| 2 | `V20260517_1210__datos_base_catalogo_usuarios.sql` | **Inserta** admin, proveedor, catálogo, zonas, campaña, almacén (sin duplicar) |
| 3 | `V20260517_1220__datos_demo_embajadores_clientes.sql` | **Inserta** 5 embajadores + 5 clientes del proveedor con flujos demo (sin duplicar) |

Idempotentes para producción con BD existente.

---

## 3. Variables requeridas (sin secretos)

Ver `database/env.required.example`.

| Variable | Obligatoria | Notas |
|----------|-------------|--------|
| `JWT_SECRET` | Sí | Mín. 32 caracteres |
| `MYSQL_USER` | Sí | |
| `MYSQL_PASSWORD` | Sí | Puede ir vacía según MariaDB local |
| `MYSQL_URL` | Sí | JDBC MariaDB, BD `gorbits` |
| `SERVER_PORT` | No | Default `8080` |
| `SPRING_PROFILES_ACTIVE` | No | Usar `mysql,prod` en servidor |

**Nuevas en esta versión:** ninguna.

---

## 4. Requisitos de runtime (para Nginx / DevOps)

- API escucha en **puerto interno 8080** (perfil `prod`).
- Rutas REST bajo prefijo **`/api/v1`** (ej. login: `POST /api/v1/auth/login`).
- Health: `GET /actuator/health`
- Frontend **separado**; el cliente Angular usa `apiBaseUrl: '/api/v1'` detrás de Nginx.

---

## 5. Credenciales demo (si se aplicó migración 1220)

| Usuario | Contraseña |
|---------|------------|
| `admin` | `admin123` |
| `proveedor` | `proveedor123` |
| `embajador01` … `embajador05` | `embajador123` |

---

## 6. Pruebas en desarrollo

- `./mvnw test` — 29 tests OK antes de entregar.
- Perfil `dev`: seeds automáticos en H2 (incl. demo si no hay `embajador01`).

---

## 7. Cambios funcionales relevantes

- Calendario de cobranza: no lista cuotas de guías en estado **DEVUELTA**.
- Perfil `prod`: desactiva Swagger y el SPA embebido del JAR.
