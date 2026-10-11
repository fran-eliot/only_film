# Auditoría CSRF — Consola H2 (P2.8)

## Alcance y amenaza

La regla SonarCloud `java:S4502` señala la omisión de tokens CSRF para `/h2-console/**` dentro de `SecurityConfig`. El objetivo es limitar esa excepción a una herramienta de administración **local**; no se permite omitir CSRF en las rutas de negocio.

## Comportamiento revisado

- El código solo ejecuta `csrf.ignoringRequestMatchers("/h2-console/**")` cuando `isLocalH2ConsoleAllowed(environment)` es verdadero.
- Esa condición exige un perfil `dev` explícito y la ausencia de `postgres`.
- El permiso de frames `sameOrigin()` se aplica únicamente bajo la misma condición.
- Los perfiles por defecto, `test`, `demo`, `postgres` y `dev+postgres` no activan la excepción CSRF.
- En el perfil `dev` la excepción se restringe al patrón `/h2-console/**`; las demás mutaciones conservan la validación CSRF de Spring Security.
- Las reglas de autorización siguen exigiendo autenticación para cualquier ruta no declarada pública; la consola H2 no aparece en la lista de rutas públicas.

## Cobertura automatizada

- `H2ConsoleCsrfProfileTest`: perfiles sin `dev`, `dev`, `postgres`, `dev+postgres`, `test`, `demo`.
- `SecurityConfigTest`: POST sin CSRF a `/h2-console/login.do` denegado fuera de desarrollo.
- `MutationCsrfSecurityTest`: operaciones de negocio sin CSRF rechazadas; autorización de administrador aplicada incluso con token.

## Riesgo residual y gestión de SonarCloud

La excepción existe por la interfaz de consola H2, que utiliza peticiones POST sin token CSRF. Su uso requiere un entorno de desarrollo confiable; no se debe activar el perfil `dev` en despliegues públicos. Este análisis del código y de los tests **no demuestra** por sí solo que las configuraciones de despliegue impidan toda exposición de H2: revisar también los perfiles y puertos de cada despliegue.

El hallazgo `java:S4502` puede seguir apareciendo porque SonarCloud identifica la llamada `ignoringRequestMatchers` de forma estática. No marcarlo resuelto ni aceptado automáticamente. Revisar manualmente la configuración real del despliegue y, tras esa revisión, registrar en SonarCloud la decisión sobre el riesgo con esta evidencia.

Seguimiento: issue #81, auditoría #49.
