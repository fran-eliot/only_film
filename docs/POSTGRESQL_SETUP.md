# PostgreSQL local (fase 1 de la issue #3)

Este paso prepara **solo la infraestructura de base de datos**. La aplicación conserva H2 como datasource por defecto hasta que incorporemos el perfil PostgreSQL y las migraciones Flyway.

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

## Pendiente

1. Revisar las seis entidades JPA y la inicialización de datos existente.
2. Añadir driver PostgreSQL, Flyway y perfil `postgres`, con Hibernate `ddl-auto=validate`.
3. Crear `V1__initial_schema.sql` y validar la migración sobre una base vacía.
4. Desactivar o adaptar `DataInitializer` en PostgreSQL para evitar inserciones duplicadas.
5. Verificar arranque y tests de integración con PostgreSQL en CI.
