# Mantenimiento de dependencias — Dependabot

## Configuración

`.github/dependabot.yml` propone actualizaciones **semanales** los lunes a las 09:00 (Europe/Madrid):

- **Maven**: dependencias y plugins declarados en el proyecto.
- **GitHub Actions**: versiones de las acciones utilizadas en `.github/workflows/`.

Se limita a cinco PR abiertos por ecosistema. Se agrupan actualizaciones `minor`/`patch` de cada ecosistema para limitar ruido; las actualizaciones `major` quedan separadas, ya que pueden requerir adaptaciones. Las alertas y actualizaciones de seguridad gestionadas por GitHub tienen reglas y tiempos propios; esta programación **no sustituye** la gestión de vulnerabilidades.

No existe fusión automática.

## Revisión recomendada

1. Leer el changelog y distinguir si la actualización afecta a runtime, tests, base de datos, seguridad o CI.
2. Comprobar los checks de regresión H2, PostgreSQL, Compose y demo seed; en cambios de Selenium, utilizar también el workflow de tests de navegador.
3. Revisar el análisis de SonarQube Cloud y los avisos de compatibilidad.
4. Para actualizaciones de Spring Boot, Java, Flyway, controladores JDBC o acciones de CI, prestar especial atención a los cambios incompatibles.
5. Aprobar y fusionar solo tras la revisión del PR; la aparición de una propuesta Dependabot **no demuestra** compatibilidad ni resuelve por sí sola una alerta.

## Optimización CI pendiente

Se ha confirmado solapamiento entre `existing-unit-and-web-tests` de `ticket-regression.yml` y `h2-regression` de `postgres-migration.yml`. No se elimina ningún job en esta etapa: antes hay que verificar nombres de checks requeridos, tipos de pruebas y tiempos reales. La mejora se abordará con una propuesta independiente.
