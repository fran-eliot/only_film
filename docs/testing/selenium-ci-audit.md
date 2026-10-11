# P2.18 — Auditoría de tests Selenium en CI

Relacionado con issues #103 y #49. Auditoría del repositorio en `main` tras P2.17, basada en `pom.xml`, `BaseSeleniumTest.java` y logs del PR #102.

## Estado comprobado

- `pom.xml` declara `selenium-java` con alcance test y utiliza JUnit Platform a través de las dependencias de Spring Boot.
- `maven-surefire-plugin` establece `rerunFailingTestsCount=2` globalmente; un test intermitente puede quedar verde tras un reintento.
- `BaseSeleniumTest` inicia Spring Boot con puerto aleatorio y crea `ChromeDriver`. Si existe la variable de entorno `CI`, añade `--headless=new`, `--no-sandbox`, `--disable-gpu` y `--disable-dev-shm-usage`.
- Los logs del PR #102 muestran que `h2-regression` ejecuta Maven con `-Dtest` y patrones de repositorio, servicio, controlador y tests de seguridad. No incluye `*SeleniumTest`. El job `ticket-regression` selecciona únicamente tres suites de seguridad/checkout.
- Las restantes ejecuciones comprueban PostgreSQL y el arranque de Docker Compose. Un smoke test HTTP no es una prueba funcional con navegador.
- Los seis jobs verdes **no prueban** que `ReviewSeleniumTest` ni las restantes pruebas Selenium se hayan ejecutado.
- Esta auditoría no acredita todavía el estado de todas las pruebas Selenium ni la existencia de un workflow Selenium independiente.

## Riesgo

Las modificaciones de UI y de sincronización de Selenium pueden integrarse sin pasar una prueba E2E real. Los reintentos globales de Surefire también pueden ocultar flakiness.

## Propuesta incremental: siguiente fase (implementación separada)

1. Inventariar las clases `*SeleniumTest`, extensiones de captura y requisitos de base de datos, navegador y perfiles.
2. Añadir un job GitHub Actions independiente para Selenium en Linux con Java 25, Chrome/Chromium disponible y ejecución headless (`CI=true`).
3. Ejecutar inicialmente una selección controlada, por ejemplo `mvn -B -ntp test -Dtest=ReviewSeleniumTest -DrerunFailingTestsCount=0`, para descubrir problemas reales antes de habilitar `*SeleniumTest`.
4. Publicar reports Surefire y capturas de fallos como artifacts cuando fallen los tests, incluso si el job fracasa.
5. Medir tiempos y estabilidad; ampliar gradualmente la cobertura y decidir si el job será obligatorio en los PR.
6. Mantener los seis jobs existentes sin cambios hasta validar el nuevo job, evitando falsos verdes y sobrecarga innecesaria.

## Criterios para dar por integrada la ejecución Selenium

- Los logs muestran nombres de suites Selenium ejecutadas y su resultado, no solo compilación.
- Hay navegador funcional en headless, los tests abren la aplicación y verifican la UI.
- Los fallos producen evidencias consultables en GitHub Actions.
- No se aceptan como prueba de estabilidad los pases obtenidos exclusivamente tras reintentos.

**Nota:** esta fase es solo auditoría y diseño; no cambia `pom.xml`, tests ni workflows.
