# PostgreSQL local (fase 1 de la issue #3)

La aplicación mantiene H2 como configuración predeterminada. El perfil opcional `postgres` utiliza PostgreSQL 17, Flyway para las migraciones y `ddl-auto=validate` para comprobar el esquema.

## Requisitos

- Docker Engine + Docker Compose
- Puerto local 5432 disponible, o cambiar `POSTGRES_PORT` en `.env`

## Aplicación completa con Docker Compose

Puedes ejecutar **Spring Boot y PostgreSQL juntos** sin instalar Java ni Maven en tu equipo:

```bash
cp .env.example .env
# Cambia POSTGRES_PASSWORD a una contraseña propia y no compartas el archivo.
docker compose up --build -d
docker compose ps
```

Abre `http://localhost:8080/movies`. Por defecto, Compose publica la web en `127.0.0.1:8080` y PostgreSQL en `127.0.0.1:5432`, ambos accesibles solo desde el propio equipo. Puedes ajustar `APP_PORT` y `POSTGRES_PORT` en `.env`.

El contenedor `app` usa `SPRING_PROFILES_ACTIVE=postgres` y se conecta a `postgres:5432` mediante la red de Compose. Espera a que PostgreSQL esté saludable antes de arrancar. Flyway crea el esquema si falta y Hibernate lo valida. En modo PostgreSQL, `DataInitializer` está desactivado: una base nueva no tendrá cartelera precargada.

Para consultar logs y reiniciar:

```bash
docker compose logs -f app
docker compose restart app
docker compose down
```

`docker compose down` conserva los volúmenes de PostgreSQL **y de los archivos subidos** (`postgres_data` y `uploads_data`). El directorio de la aplicación `/app/uploads` se guarda en un volumen Docker: las imágenes de perfil y películas sobreviven a los reinicios y a la recreación del contenedor `app`. Para ejecución fuera de Docker, la ruta predeterminada sigue siendo `uploads/`; puede cambiarse con `ONLYFILM_UPLOAD_DIR`. **No ejecutes `docker compose down -v` si necesitas los datos o las imágenes**: elimina ambos volúmenes y su contenido. Las imágenes preexistentes del directorio local `uploads/` del repositorio no se copian automáticamente al volumen Docker; realiza una migración explícita si necesitas conservarlas.

Para arrancar solo la base de datos y ejecutar Spring Boot desde el IDE, utiliza `docker compose up -d postgres` y las instrucciones del apartado siguiente.

### Seguridad y límites

Este Compose está diseñado para **desarrollo local**, no para un despliegue de producción. No expongas `APP_PORT` ni `POSTGRES_PORT` a redes públicas; mantén `.env` fuera de Git. La aplicación todavía no cuenta con una estrategia definitiva de persistencia de ficheros subidos al contenedor.

## Arranque solo de PostgreSQL

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

El job `postgres-schema` comprueba tanto el arranque inicial como el reinicio de Spring Boot sobre la misma instancia PostgreSQL: inserta un registro de prueba, reinicia el contenedor PostgreSQL y la aplicación, y confirma que el registro permanece y que la migración V1 figura una sola vez como completada en `flyway_schema_history`. Consulta la última ejecución del workflow para confirmar su resultado.

La integración JPA con PostgreSQL y las pruebas de concurrencia se implementaron en la issue #5. El workflow `compose-smoke.yml` comprueba además el arranque conjunto de contenedores y la persistencia tras reinicios.

## Datos de demostración opcionales (issue #10)

El perfil PostgreSQL **no** genera datos de muestra por defecto. Para poblar una base **nueva y vacía** con películas, salas, sesiones, tickets y cuentas ficticias, activa expresamente los dos perfiles:

```bash
SPRING_PROFILES_ACTIVE=postgres,demo DB_URL=jdbc:postgresql://localhost:5432/onlyfilm DB_USERNAME=onlyfilm DB_PASSWORD='<contraseña-local>' ./mvnw spring-boot:run
```

En Docker Compose, añade temporalmente `SPRING_PROFILES_ACTIVE=postgres,demo` al servicio `app` en un archivo override local **no versionado**; después de sembrar, vuelve a `postgres` y recrea el servicio.

El sembrado se omite si **cualquiera** de las seis tablas de negocio tiene registros. El proceso está envuelto en una transacción para evitar conjuntos parciales tras errores. Al reiniciar con `postgres,demo` no se duplican registros. **Solo para desarrollo local:** las cuentas demo tienen contraseñas conocidas dentro del código, por lo que no deben activarse en entornos accesibles públicamente. No ejecutes este procedimiento contra una base con datos reales.
