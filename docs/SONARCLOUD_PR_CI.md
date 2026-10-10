# SonarCloud en pull requests — Validación P1

## Integración sin ejecutar dos veces las pruebas

El job `h2-regression` de `.github/workflows/postgres-migration.yml` compila y ejecuta la selección de regresión H2. JaCoCo publica su informe en el artefacto `jacoco-h2-regression`. En los PR internos hacia `main`, el mismo runner utiliza a continuación las clases compiladas y el informe JaCoCo para invocar el scanner Maven de SonarCloud **sin volver a ejecutar los tests**.

La identidad del proyecto propio está definida en `pom.xml`: `sonar.projectKey=fran-eliot_only_film` y `sonar.organization=fran-eliot`. Proyecto: https://sonarcloud.io/project/overview?id=fran-eliot_only_film. El análisis necesita `SONAR_TOKEN` como secreto del repositorio; no se imprime ni se versiona.

El workflow `.github/workflows/sonar.yml` continúa siendo una ejecución manual independiente, no se activa automáticamente en los PR.

## Comportamiento y límites

- En PRs internos con `SONAR_TOKEN`, se intenta el análisis SonarCloud. Un fallo del scanner hace fallar el job.
- En PRs de forks o cuando falta `SONAR_TOKEN`, el análisis se omite con un aviso explícito. **Un check verde en ese caso no acredita SonarCloud**.
- El análisis consume el informe de la selección H2, no el de Selenium ni el de todos los tests posibles. No hay nueva ejecución de Maven `test`.
- No se introduce un umbral adicional de cobertura ni se cambia el Quality Gate remoto desde el repositorio.
- Revisar el resultado en el enlace del PR dentro de SonarCloud y comprobar que el análisis corresponde al PR correcto. En particular, validar permisos del proyecto, token, cobertura importada y reglas de código nuevo.

## Validación pendiente

Para declarar la integración plenamente operativa es imprescindible una ejecución real que confirme la subida del análisis del PR y la correspondencia con el proyecto SonarCloud. El proyecto propio ya está creado y se ha actualizado el `pom.xml`. Queda confirmar mediante el workflow que el scanner puede publicar el análisis del PR con el secreto `SONAR_TOKEN` y que importa JaCoCo. Si no aparece el análisis, revisar también si el análisis automático del proyecto está activado y podría interferir con el análisis de CI.
