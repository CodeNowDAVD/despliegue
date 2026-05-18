# Migraciones SQL — GOrbitS

Entrega del **backend developer**: scripts versionados para que **DevOps/servidor** los aplique en MariaDB/MySQL.

## Orden de ejecución

1. `V20260517_1200__esquema_inicial_gorbits.sql` — tablas (`IF NOT EXISTS`) + `schema_migrations`
2. `V20260517_1210__datos_base_catalogo_usuarios.sql` — catálogo y cuentas base (idempotente)
3. `V20260517_1220__datos_demo_embajadores_clientes.sql` — datos demo opcionales (idempotente)

Seguras en base **gorbits** ya existente: no borran datos; solo crean/añaden lo que falte.

## Registro de versiones

Recomendación para el servidor: tabla `schema_migrations` (creada en la migración 1200) y aplicar cada `V*.sql` una sola vez.

## Variables de entorno

Ver `env.required.example`.
