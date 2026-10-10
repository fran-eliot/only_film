# Línea base de cobertura JaCoCo — Regresión H2

## Alcance y procedencia

El job `h2-regression` de `.github/workflows/postgres-migration.yml` ejecuta una selección concreta de pruebas mediante Maven Surefire con `-DrerunFailingTestsCount=0`. El plugin `jacoco-maven-plugin` configurado en `pom.xml` instrumenta el código y genera informes HTML/XML durante `mvn test`.

El job publica `target/site/jacoco/` y `target/surefire-reports/` como artefacto **jacoco-h2-regression**, que se conserva durante 14 días. El paso usa `if: always()`, de modo que los informes que se hayan generado se intentan publicar incluso si fallan las pruebas.

Ruta en GitHub: **Actions → PostgreSQL schema and regression validation → ejecución del PR → Artifacts → jacoco-h2-regression**. Tras descargarlo, se puede abrir `target/site/jacoco/index.html` o consultar `target/site/jacoco/jacoco.xml` para obtener contadores procesables.

**Importante:** esta medición representa la selección de pruebas de regresión H2, no la cobertura global de todas las pruebas del repositorio.

## Reproducción local

Con Java 25 y Maven disponibles, desde la raíz del repositorio:

```bash
mvn -B -ntp clean test -Dtest='*RepositoryTest,*ServiceTest,*ControllerTest,*SecurityConfigTest,TicketSecurityRegressionTest,TicketCheckoutRegressionTest,TicketCheckoutConcurrencyTest,MutationCsrfSecurityTest,EnvironmentHardeningTest,SessionManagementRollbackTest,SessionEditInvariantsTest,MvcErrorContractTest' -DrerunFailingTestsCount=0
```

Los informes HTML y XML se generan en `target/site/jacoco/`. El comando `clean` evita mezclar datos de ejecuciones locales anteriores; en Actions el job se ejecuta en un runner nuevo.

## Interpretación y siguientes pasos

- Registrar porcentajes de líneas y ramas **solo después de inspeccionar un informe real**.
- Este job no incluye las pruebas de navegador Selenium ni todas las clases fuera del selector de Surefire; otros workflows validan PostgreSQL y más escenarios.
- No se obtiene una cobertura global promediando los porcentajes de jobs o JVM independientes. Para ello haría falta una ejecución unificada o agregación correcta de datos JaCoCo.
- Un porcentaje alto no garantiza las invariantes de negocio. Conviene revisar especialmente concurrencia de checkout, autorización de compras, edición de sesiones, unicidad de butacas y edición administrativa de entradas.
- Este cambio no establece umbrales mínimos: los objetivos para código nuevo se decidirán a partir de resultados comprobados.
