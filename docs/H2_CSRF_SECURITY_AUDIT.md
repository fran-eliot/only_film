# Excepción CSRF de la consola H2 (P2.4)

## Contexto

La aplicación usa formularios Spring MVC/Thymeleaf y conserva la **protección CSRF de Spring Security** en sus operaciones de modificación. La consola de H2 realiza peticiones POST que no incluyen tokens CSRF.

La única excepción configurada en `SecurityConfig` es `/h2-console/**`; se activa **exclusivamente** si el perfil `dev` está activo **y** `postgres` no está activo. En este mismo caso, la política de frames se limita a `sameOrigin` para permitir la consola. No existe una excepción global.

| Perfiles | Excepción CSRF H2 | Nota |
| --- | --- | --- |
| Predeterminado | No | Consola H2 deshabilitada en application.properties |
| `dev` | Sí, solo `/h2-console/**` | Perfil local voluntario |
| `postgres` | No | Consola H2 deshabilitada; CSRF activo |
| `dev,postgres` | No | El perfil PostgreSQL prevalece para esta excepción |

**Riesgo residual:** el perfil `dev` desactiva CSRF sobre la ruta de la consola H2. No habilitarlo en servicios públicos, máquinas compartidas ni entornos de producción. El selector de perfiles no comprueba por sí solo si la aplicación está publicada en Internet.

## Cobertura

- `H2ConsoleCsrfProfileTest` verifica las cuatro combinaciones de perfiles anteriores.
- `SecurityConfigTest` comprueba un POST sin token contra `/h2-console/login.do` con perfil estándar.
- `MutationCsrfSecurityTest` comprueba que los POST sensibles de la aplicación rechazan peticiones sin token y que las autorizaciones siguen vigentes.

## SonarQube Cloud

La regla `java:S4502` puede señalar `ignoringRequestMatchers` aunque esté estrictamente condicionada. Su advertencia obliga a revisar el contexto; ni la presencia de la alerta acredita una explotación, ni el paso de los tests elimina por sí solo el riesgo de publicar un entorno `dev`. Si el hallazgo continúa, revisarlo en SonarQube contra esta justificación antes de clasificarlo como aceptado.
