# 🎬 Only Film

Aplicación web de gestión cinematográfica desarrollada con **Java 25**, **Spring Boot 4**, **Spring MVC**, **Spring Security** y **Thymeleaf**. Permite consultar la cartelera, gestionar proyecciones, seleccionar butacas, comprar entradas y administrar un cine con permisos por rol.

Este repositorio conserva la base de un **proyecto académico colaborativo** y documenta su evolución posterior, especialmente en seguridad, pruebas automatizadas, PostgreSQL, Flyway y Docker Compose.

![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot 4.0.5](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-336791)
![Flyway](https://img.shields.io/badge/Migrations-Flyway-blue)
![Docker Compose](https://img.shields.io/badge/Docker-Compose-2496ED)

## Capturas de la aplicación

| Cartelera | Detalle de película |
| --- | --- |
| ![Cartelera](docs/screenshots/cartelera.png) | ![Detalle](docs/screenshots/pelicula.png) |

| Selección de butacas | Compra y ticket QR |
| --- | --- |
| ![Butacas](docs/screenshots/sesion.png) | ![Ticket](docs/screenshots/compra-qr.png) |

Más capturas en [`docs/screenshots/`](docs/screenshots/).

## Funcionalidades

- **Visitantes:** consultar películas y sesiones, registrarse e iniciar sesión.
- **Usuarios:** reservar/comprar entradas, ver sus tickets, consultar el código QR y publicar reseñas de películas para las que tienen una entrada pagada.
- **Administradores:** gestionar películas, salas, sesiones y usuarios; consultar las entradas vendidas y moderar reseñas.

El checkout protege la disponibilidad de las butacas mediante operaciones transaccionales y bloqueo en la base de datos para evitar la doble venta ante peticiones concurrentes. Los usuarios solo pueden consultar sus propias entradas, salvo los administradores.

## Tecnologías y arquitectura

| Capa | Tecnologías |
| --- | --- |
| Backend | Java 25, Spring Boot 4.0.5, Spring MVC, Spring Data JPA, Hibernate |
| Seguridad | Spring Security, BCrypt, autorización por roles y CSRF |
| Frontend | Thymeleaf, Bootstrap 5, HTML, CSS, JavaScript |
| Persistencia local rápida | H2 en memoria (esquema gestionado por Hibernate) |
| Persistencia duradera | PostgreSQL 17, migraciones Flyway, Hibernate `validate` |
| Pruebas | JUnit 5, Mockito, MockMvc, tests JPA con PostgreSQL, Selenium |
| Infraestructura | Dockerfile multi-stage Java 25, Docker Compose, GitHub Actions, JaCoCo |

Flujo principal: **Controlador MVC → servicio / reglas de negocio → repositorio JPA → base de datos**. La aplicación utiliza las entidades `Movie`, `Room`, `Session`, `Ticket`, `Review` y `User`.

## Inicio rápido: aplicación y PostgreSQL con Docker

Solo necesitas **Docker Engine y Docker Compose**.

```bash
git clone https://github.com/fran-eliot/only_film.git
cd only_film
cp .env.example .env
# Edita .env y establece POSTGRES_PASSWORD con un valor privado y robusto
docker compose up --build -d
docker compose ps
```

Visita **http://localhost:8080/movies**.

- El servicio Spring Boot usa el perfil `postgres` y se conecta al servicio `postgres` de Compose.
- PostgreSQL aplica automáticamente las migraciones de [`db/migration/`](src/main/resources/db/migration/) con Flyway. Hibernate valida el esquema.
- Los datos de PostgreSQL y las imágenes subidas sobreviven a la recreación de contenedores gracias a los volúmenes `postgres_data` y `uploads_data`.
- Los puertos de la aplicación y de PostgreSQL solo se publican en `127.0.0.1`; pueden ajustarse con `APP_PORT` y `POSTGRES_PORT` en `.env`.
- La base PostgreSQL **no se rellena con usuarios o películas demo por defecto**.

```bash
docker compose logs -f app   # Consultar logs
docker compose down          # Detener sin borrar datos
```

**Atención:** `docker compose down -v` elimina los volúmenes y sus datos. Este Compose se concibió para **desarrollo local**, no como configuración productiva expuesta a Internet.

Consulta la [guía detallada de PostgreSQL y Docker Compose](docs/POSTGRESQL_SETUP.md).

## Desarrollo con Java y Maven

Requiere **JDK 25** y Maven (o Maven Wrapper si está disponible en tu entorno).

```bash
# H2 en memoria, sin datos duraderos
./mvnw spring-boot:run

# Herramientas exclusivamente locales: consola H2, logging SQL, caché de plantillas desactivada
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

En Windows/PowerShell configura `$env:SPRING_PROFILES_ACTIVE = 'dev'` y ejecuta `.\mvnw.cmd spring-boot:run`. En Windows CMD, usa `mvnw.cmd`.

### Perfiles Spring

| Perfil | Uso | Precauciones |
| --- | --- | --- |
| Predeterminado | H2 en memoria, consola deshabilitada | Los datos se pierden al cerrar la aplicación |
| `dev` | Desarrollo local con consola H2 y SQL logging | No activar en servidores expuestos |
| `postgres` | PostgreSQL + Flyway + validación de esquema | Necesita `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` |
| `postgres,demo` | Cargar datos ficticios en PostgreSQL vacío | **Solo desarrollo local**; genera cuentas con contraseñas conocidas |

Para usar PostgreSQL desde Java fuera de Compose, ejecuta primero `docker compose up -d postgres` y configura las variables anteriores apuntando al puerto local. Las instrucciones completas están en [`POSTGRESQL_SETUP.md`](docs/POSTGRESQL_SETUP.md).

La carga demo solo se ejecuta cuando se solicita explícitamente y se omite si las tablas de negocio ya contienen registros. No utilizar ese perfil contra datos reales.

## Seguridad y límites actuales

La implementación incluye:

- Contraseñas cifradas con **BCrypt**, sesión autenticada y permisos `ROLE_USER` / `ROLE_ADMIN`.
- Comprobaciones de propiedad de entradas y reseñas, incluidas las operaciones de modificación por POST.
- Formularios POST con protección **CSRF** para cambios de estado; las rutas GET no desactivan ni eliminan recursos.
- Checkout transaccional, validaciones de estado de tickets y pruebas de concurrencia.
- Subida restringida a imágenes **JPEG/PNG verificadas por contenido real**, recodificadas, con nombres aleatorios y límites de tamaño y dimensiones.
- Consola H2, logs SQL y excepciones específicas de CSRF deshabilitados fuera del perfil local `dev`.

No se ha integrado una pasarela de pago real; el checkout representa un flujo de compra de demostración. Tampoco se presenta esta configuración como una auditoría de seguridad completa ni un despliegue productivo endurecido.

Ver la [auditoría de consolidación técnica](docs/SECURITY_HARDENING_AUDIT.md) para el contexto de las mejoras.

## Pruebas y CI

```bash
./mvnw test

# Por tipo de test
./mvnw test -Dtest='*ServiceTest'
./mvnw test -Dtest='*ControllerTest'
./mvnw test -Dtest='*RepositoryTest'
```

Algunas pruebas necesitan servicios externos, entorno gráfico/headless o configuración específica: consulta los workflows y la [guía de tests JPA en PostgreSQL](docs/POSTGRESQL_INTEGRATION_TESTS.md).

| Workflow | Qué comprueba |
| --- | --- |
| [`ticket-regression.yml`](.github/workflows/ticket-regression.yml) | Tests de servicios, repositorios, controladores, seguridad y checkout |
| [`postgres-migration.yml`](.github/workflows/postgres-migration.yml) | Migración real Flyway, esquema, persistencia, integración JPA y concurrencia |
| [`compose-smoke.yml`](.github/workflows/compose-smoke.yml) | Build/arranque de contenedores y persistencia tras reiniciar o recrear |
| [`postgres-demo-seed.yml`](.github/workflows/postgres-demo-seed.yml) | Base vacía por defecto, demo opcional y ausencia de duplicados |
| [`tests.yml`](.github/workflows/tests.yml) | Suite manual, incluidos jobs Selenium |
| [`sonar.yml`](.github/workflows/sonar.yml) | Análisis estático Sonar |

Los checks principales se ejecutan sobre pull requests dirigidos a `main`. El workflow general `tests.yml` es manual; los E2E de Selenium no deben confundirse con las regresiones incluidas en todos los PR. Consulta [GitHub Actions](https://github.com/fran-eliot/only_film/actions) para resultados vigentes. Se han retirado del README los recuentos de tests y porcentajes de cobertura históricos porque pueden variar y no se ha verificado aquí una medición consolidada reciente.

## Organización del repositorio

```text
src/
  main/
    java/com/demo/       # Controllers, services, repositories, entidades, seguridad
    resources/
      templates/         # Vistas Thymeleaf
      static/            # CSS, JS e imágenes estáticas
      db/migration/      # Migraciones Flyway
  test/java/com/demo/    # Tests JUnit / MockMvc / JPA / seguridad
docs/                    # Arquitectura, guías, auditoría y capturas
.github/workflows/       # CI
Dockerfile
compose.yaml
.env.example
```

## Origen, créditos y evolución

Only Film deriva del proyecto colaborativo de formación [**certidevs/g1_testing**](https://github.com/certidevs/g1_testing). Se reconoce el trabajo del equipo original:

- [Fran Ramírez Martín](https://github.com/fran-eliot)
- [Adrián López de Haro](https://github.com/alopezdeharo)
- [Barbara Urbano](https://github.com/barbieurbano)
- [Andrés Soto](https://github.com/mrandressoto-code)

Este repositorio [**fran-eliot/only_film**](https://github.com/fran-eliot/only_film) recoge una evolución técnica posterior; el historial de Git y los pull requests permiten atribuir los cambios con precisión. No se atribuye en exclusiva el trabajo académico original.

## Próximas líneas de evolución

API REST, integración con un proveedor real de pagos, observabilidad, automatización del despliegue y mejoras continuas de mantenibilidad. Son posibles ampliaciones, **no funcionalidades actualmente implantadas**.

**Licencia:** proyecto de origen académico; consulta el repositorio original y la documentación del proyecto para condiciones de uso. Este README no establece por sí solo una licencia adicional.
