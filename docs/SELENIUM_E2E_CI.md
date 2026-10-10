# Ejecución manual de pruebas Selenium E2E

El workflow `.github/workflows/tests.yml` se ejecuta manualmente mediante `workflow_dispatch`. Además, el job Selenium se ejecuta automáticamente en los PR hacia `main` **solo cuando cambia `.github/workflows/tests.yml`**, para comprobar la configuración antes de integrarla. Los otros cuatro jobs se ejecutan únicamente en modo manual, lo que evita duplicar las pruebas de regresión habituales.

## Ejecución desde GitHub

1. Abrir **Actions → Tests (manual)**.
2. Seleccionar **Run workflow** y la rama que se quiere probar.
3. Ejecutar el workflow y consultar el job **Tests de Selenium E2E**.

El job ejecuta `mvn -B -ntp test -Dtest='*SeleniumTest' -DrerunFailingTestsCount=0`, que selecciona las cinco clases E2E actuales: `CheckoutSeleniumTest`, `MovieSeleniumTest`, `ReviewSeleniumTest`, `TicketSeleniumTest` y `UserSeleniumTest`. No ejecuta la clase base `BaseSeleniumTest`, ni depende de parámetros de entrada no declarados.

En GitHub Actions, la variable `CI` activa Chrome headless en `BaseSeleniumTest`. Se comprueba la versión de Chrome del runner antes de ejecutar las pruebas. La selección es fija para evitar parámetros manuales ambiguos.

## Evidencias y diagnóstico

- El job sube `target/surefire-reports/` como artifact, incluso ante fallos.
- Si el job falla, intenta subir capturas desde `target/screenshots/`.
- Los reintentos de Surefire están desactivados **en este job** para distinguir los fallos reales de las intermitencias. Esto no modifica la configuración Maven de otros jobs.
- Los artifacts se conservan durante siete días.
- Si no aparecen capturas, comprobar el fallo en los logs y si se llegó a inicializar Chrome; la extensión de capturas solo dispone de un navegador cuando su inicialización ha finalizado.

## Validación pendiente

Cuando un PR modifica este workflow, el job Selenium se ejecuta automáticamente; es necesario comprobar su resultado y los informes antes del merge. Para repetir las pruebas tras una integración, se puede lanzar el workflow manualmente desde Actions. Un único resultado satisfactorio no demuestra ausencia permanente de intermitencias.
