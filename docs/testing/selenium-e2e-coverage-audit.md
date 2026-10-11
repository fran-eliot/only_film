# P2.22 — Auditoría de cobertura E2E y evidencias Selenium

Seguimiento de #111 y #49. Estado del repositorio tras la integración de P2.21.

## Inventario de escenarios automatizados

| Suite | Casos | Cobertura confirmada por código |
| --- | ---: | --- |
| `MovieSeleniumTest` | 2 | Listado y navegación a detalle; detalle de película |
| `ReviewSeleniumTest` | 4 | Crear reseña; validación sin puntuación; navegación al formulario; selección de estrellas |
| `TicketSeleniumTest` | 7 | Listado propio; detalle y QR; formulario checkout; snacks y totales; pago y confirmación; navegación a checkout; edición de descuento como administrador |
| **Total** | **13** | Suites ejecutadas en un job Selenium headless del workflow actual |

La ejecución anterior de CI confirmó 13/13 sin reintentos. Esto **no implica cobertura exhaustiva** de la aplicación: no hay evidencia en estas suites de pruebas E2E para administración completa de sesiones y salas, subida de imágenes o todas las variantes de error/seguridad.

## Infraestructura y duplicaciones

- `BaseSeleniumTest`: arranca Spring Boot en puerto aleatorio, crea ChromeDriver headless si `CI` está definido, crea películas comunes y limpia repositorios. Usa `@BeforeEach` y `@AfterEach`.
- Las suites Review y Ticket tienen además un `@BeforeEach` que vuelve a limpiar repositorios y crea datos específicos. Revisar esta doble preparación antes de cambiarla: modificar el orden podría introducir dependencias ocultas.
- Review y Ticket duplican parte del login y de la lógica de clic seguro; sus métodos no son idénticos. No extraer todavía sin contrastar sus condiciones y sus tests.
- `ScreenshotOnFailure` guarda capturas en `target/screenshots` cuando el test lanza excepción. Los reports Surefire quedan en `target/surefire-reports`.
- `pom.xml` define `surefire.rerunFailingTestsCount`, que el workflow Selenium establece a 0. Comprobar siempre que los logs no indiquen `Flakes`.
- Un único job secuencial comparte runner y configuración. Separar suites aumentaría aislamiento y diagnóstico pero también consumo/tiempo de setup; decidir con datos de estabilidad.

## Mejora implementada en P2.22

El workflow publica `target/surefire-reports/` y `target/screenshots/` usando `if: always()`: así se conservan los resultados incluso cuando pasan todos los tests, mientras que las capturas solo se generan ante fallos. Retención de siete días, sin modificar suites o lógica de negocio.

## Próximas mejoras candidatas

1. Refactorizar login común y clicar elementos **solo después** de comparar flujos de Review y Ticket y preservar sus escenarios.
2. Evaluar reducir la doble limpieza en `@BeforeEach` y añadir aislamiento explícito antes de paralelizar.
3. Añadir escenarios E2E para los flujos críticos no cubiertos según prioridades funcionales.
4. Medir duración, reintentos y fallos de cada suite; separar jobs si los beneficios compensan.

**Criterio para fusionar P2.22:** siete jobs correctos, 13 Selenium realmente ejecutados sin reintentos, artifact de informes disponible tras éxito, sin regresiones de otros jobs.
