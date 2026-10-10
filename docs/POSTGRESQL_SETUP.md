# PostgreSQL local (fase 1 de la issue #3)

La aplicación mantiene H2 como configuración predeterminada. El perfil opcional `postgres` utiliza PostgreSQL 17, Flyway para las migraciones y `ddl-auto=validate` para comprobar el esquema.

## Requisitos

- Docker Engine + Docker Compose
- Puerto local 5432 disponible, o cambiar `POSTGRES_PORT` en `.env`

## Arranque

```bash
cp .env.example .env
# Edita .env para configurar POSTGRES_PASSWORD con un valor propio.
docker compose up -d postgres
docker compose ps
```

El contenedor incluye healthcheck. PostgreSQL persiste sus datos en el volumen `postgres_data`.

Para detener sin borrar los datos:

```bash
docker compose down
```

Para destruir expresamente la base local y reiniciarla desde cero (operación destructiva):

```bash
docker compose down -v
```

**Seguridad:** no subir el archivo `.env`; Compose solo expone PostgreSQL en `127.0.0.1`. Los valores de `.env.example` son marcadores de posición, nunca secretos utilizables.

## Arranque de Only Film con PostgreSQL

Al arrancar PostgreSQL con Compose, configura estas variables para el proceso de Java (no son variables de Spring leídas automáticamente del archivo `.env`):

```bash
export SPRING_PROFILES_ACTIVE=postgres
export DB_URL=jdbc:postgresql://localhost:5432/onlyfilm
export DB_USERNAME=onlyfilm
export DB_PASSWORD='tu-contrasena-del-archivo-env'
./mvnw spring-boot:run
```

En PowerShell utiliza `$env:NOMBRE_VARIABLE = 'valor'` y `./mvnw.cmd spring-boot:run`.

Flyway aplica `V1__initial_schema.sql` en una base vacía. El perfil `postgres` desactiva los datos de demostración (`DataInitializer`) y Hibernate solo valida el esquema. **No ejecutes `docker compose down -v` sobre bases que quieras conservar.**

## Validación

El workflow `postgres-migration.yml` ejecuta los tests habituales con H2 y arranca la aplicación contra PostgreSQL real en CI para validar las migraciones y el esquema. Consulta los resultados de Actions antes de dar por completada la migración.

**Pendiente:** comprobar la ejecución real y corregir posibles discrepancias de Flyway/Hibernate. Se aconseja incorporar pruebas persistentes de repositorios contra PostgreSQL en una fase posterior.
