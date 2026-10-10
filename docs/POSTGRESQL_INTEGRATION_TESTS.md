# Tests de integración con PostgreSQL (issue #5)

La clase `PostgresPersistenceIntegrationTest` ejecuta pruebas **reales** sobre PostgreSQL 17:

1. Persistencia de `Movie`, `Room`, `Session`, `Ticket` y `User`, navegación de relaciones y consultas derivadas de Spring Data JPA.
2. Restricción de unicidad del correo en la tabla `users`.
3. Dos compras simultáneas de la misma entrada: debe existir exactamente una compra exitosa, con ticket `PAGADO`.

No utiliza `@Transactional` a nivel de clase: las transacciones de compra en hilos independientes deben poder ver los fixtures ya confirmados.

## CI

El job `PostgreSQL JPA and concurrent checkout` del workflow `postgres-migration.yml` proporciona PostgreSQL 17 y ejecuta únicamente la clase de integración, utilizando el perfil `postgres`. Flyway aplica V1 antes de crear el `EntityManagerFactory` y Hibernate valida el esquema.

Las pruebas existentes sobre H2 siguen ejecutándose en su job independiente.

## Local

Arranca PostgreSQL con `docker compose up -d postgres` y un archivo `.env` privado. Configura las variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` para el proceso Maven y ejecuta:

```bash
./mvnw test -Dtest=PostgresPersistenceIntegrationTest
```

Este test usa `@ActiveProfiles("postgres")`, por lo que no requiere activar manualmente el perfil. **Nunca ejecutes las pruebas contra una base de datos con información real**: crean filas y no las limpian al terminar. Usa una base PostgreSQL de pruebas desechable.

La contraseña literal del workflow es una credencial efímera de CI, no un secreto de producción.
