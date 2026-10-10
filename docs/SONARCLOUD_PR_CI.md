# SonarQube Cloud — Análisis de PR y rama principal

## Integración

El job `h2-regression` de `.github/workflows/postgres-migration.yml` ejecuta los tests de regresión H2, genera JaCoCo y publica el artefacto `jacoco-h2-regression`. En el **mismo job** se ejecuta el scanner Maven sin volver a ejecutar pruebas.

El análisis automático se activa en dos situaciones:
- Pull requests hacia `main`: informa de la calidad del código nuevo respecto a la rama de destino.
- Push a `main`: actualiza la línea base de la rama principal, incluidos los indicadores del código existente y la cobertura disponible.

El workflow `.github/workflows/sonar.yml` sigue siendo manual; no se añade otro pipeline automático de tests.

## Identidad y recursos analizados

Proyecto: https://sonarcloud.io/project/overview?id=fran-eliot_only_film

- `sonar.organization=fran-eliot`
- `sonar.projectKey=fran-eliot_only_film`
- `sonar.sources=src/main/java,src/main/resources/templates` para incluir las plantillas Thymeleaf en el análisis, especialmente las comprobaciones de seguridad XSS.
- `sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml` para importar el informe creado por el job H2.

**La cobertura importada procede de la selección H2**, no de toda la batería de tests (por ejemplo, no incluye Selenium). Los porcentajes deben interpretarse con esa limitación. Un PR sin líneas de código de producción nuevas puede mostrar «0 nuevas líneas» sin que ello signifique que el análisis de código existente haya fallado.

## Credenciales, seguridad y Quality Gate

El scanner requiere el secreto de GitHub Actions `SONAR_TOKEN`, nunca versionado. En PR de forks GitHub puede no proporcionar dicho secreto: en ese caso el scanner se omite con un aviso explícito y un job verde **no demuestra** análisis de SonarQube. Si el scanner se ejecuta y falla, el job falla.

El proyecto SonarQube Cloud tiene desactivado **Automatic Analysis**, para evitar conflictos con el análisis desde CI. El Quality Gate se administra en SonarQube Cloud; aquí no se introducen umbrales arbitrarios ni se presupone que un análisis subido equivale a un Quality Gate aprobado.

## Comprobación

1. En un PR, verificar los logs del job H2: tests, JaCoCo, sensor Thymeleaf y `ANALYSIS SUCCESSFUL`. Comprobar el dashboard del PR y su Quality Gate.
2. Tras fusionar en `main`, comprobar un run disparado por `push` y que el dashboard de `main` actualiza su análisis. **Hasta la primera ejecución sobre main, esta funcionalidad no se considera validada de extremo a extremo.**
3. Revisar la cobertura y las incidencias sin equiparar cobertura parcial H2 con cobertura global del repositorio.
