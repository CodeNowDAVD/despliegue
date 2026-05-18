# Base de datos GOrbitS (MySQL / MariaDB)

## Importar desde cero

1. Crea un esquema vacío (o usa el que te dé el hosting). El script opcional incluye `CREATE DATABASE` comentado.
2. Ejecuta el seed completo:

```bash
mysql -u TU_USUARIO -p TU_BASE_DE_DATOS < db/mysql/gorbits_seed.sql
```

En phpMyAdmin: pestaña **SQL** → pegar el archivo o usar **Importar**.

3. Arranca la aplicación Spring Boot con **`--spring.profiles.active=mysql`** (véase `application-mysql.properties`). Variables opcionales: `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`. El proyecto usa el driver **`mariadb-java-client`** (recomendado para MariaDB y Termux).

## Credenciales de demostración (tras importar)

| Usuario     | Contraseña     | Rol       |
|-------------|----------------|-----------|
| `admin`     | `admin123`     | ADMIN     |
| `proveedor` | `proveedor123` | PROVEEDOR |

Los `password_hash` son **BCrypt** (compatibles con Spring Security del proyecto).

## Nota

Este script coincide con el **modelo JPA actual** (`spring.jpa.hibernate.ddl-auto`-equivalent). Si cambiáis entidades en código, revisad o regenerad las tablas.
