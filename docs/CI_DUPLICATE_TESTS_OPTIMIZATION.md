# Optimización CI: tests redundantes

## Justificación

En los PR a `main`, el job `existing-unit-and-web-tests` del workflow `ticket-regression.yml` ejecutaba:
`*RepositoryTest,*ServiceTest,*ControllerTest,*SecurityConfigTest,MutationCsrfSecurityTest,EnvironmentHardeningTest,SessionManagementRollbackTest,SessionEditInvariantsTest,MvcErrorContractTest`.

El job `h2-regression` de `postgres-migration.yml` ejecuta exactamente estos mismos selectores, más `TicketSecurityRegressionTest,TicketCheckoutRegressionTest,TicketCheckoutConcurrencyTest`. De esta forma, `existing-unit-and-web-tests` volvía a compilar y repetir un subconjunto de pruebas que ya cubre H2.

Se elimina **solo** el job `existing-unit-and-web-tests`. Se conserva el job independiente `ticket-regression`, además de H2, JaCoCo, SonarQube, integración PostgreSQL, validación de migraciones, Docker Compose y datos demo.

## Validación y riesgo

- El cambio reduce de siete a **seis** los jobs de regresión habituales por PR, si todos se programan como antes.
- La protección clásica de `main` no pudo inspeccionarse: GitHub devolvió `403 Resource not accessible by integration`. **Antes de fusionar**, comprobar en **Settings → Branches → Branch protection rules** que `existing-unit-and-web-tests` no es un check obligatorio. Si lo es, actualizar expresamente la protección para requerir `h2-regression` o mantener el job.
- Verificar que los seis jobs restantes finalizan con éxito en el PR. La comparativa de tiempos entre ejecuciones se puede documentar una vez concluida la ejecución.
- Esta etapa **no** elimina la duplicación parcial entre el job `ticket-regression` y `h2-regression`; la conservamos deliberadamente como check específico de negocio.
