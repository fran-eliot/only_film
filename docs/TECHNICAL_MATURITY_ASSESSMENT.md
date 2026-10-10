# Evaluación de madurez técnica — Only Film

**Fecha:** 2026-10-10  
**Base analizada:** `main`, commit `9f2a31e927df105d14e3d30d4d8277fa61883deb` (PR #32).  
**Método:** revisión estática de código, tests versionados, `pom.xml`, Flyway, Dockerfile, GitHub Actions, README y configuración. **No** constituye una prueba de penetración, un análisis SonarCloud reciente ni una medición de cobertura.

## Dictamen ejecutivo

**Estado cualitativo: proyecto académico evolucionado, técnicamente defendible como portfolio backend junior, pero todavía no preparado para producción pública sin una nueva ronda de controles y validación.**

El sistema muestra casos de uso con valor real (checkout transaccional y con control de concurrencia, roles, persistencia PostgreSQL/Flyway, subida segura de imágenes y despliegue local reproducible). La mayoría de mejoras anteriores se han integrado mediante PR con pruebas verdes. No hay una cifra de cobertura reciente ni resultados verificados de Selenium, SonarCloud y escáneres de dependencias en el proceso automático de PR.

### Matriz de valoración

| Dimensión | Diagnóstico | Evidencia | Brecha relevante |
|---|---|---|---|
| Arquitectura | **Buena base** | MVC, DTOs de películas/salas/sesiones, `SessionManagementService` y servicios de usuario/checkout | Hay caminos administrativos y de reseñas que aún enlazan entidades JPA y guardan desde controladores |
| Seguridad de aplicación | **Ha mejorado considerablemente** | CSRF, roles, comprobación de propiedad, BCrypt, validación/recodificación JPEG/PNG, separación de perfiles | `POST /tickets/save` acepta `@ModelAttribute Ticket` completo; revisar invariantes de inventario, campos mutables y privilegios |
| Integridad de datos | **Buena en checkout y creación** | Bloqueo pesimista de tickets, operaciones transaccionales, tests de rollback | `sessions` permite cambiar película, precio y horario aun existiendo entradas; falta regla explícita. No hay `UNIQUE(session_id,fila,seat)` en migración V1 |
| Pruebas | **Amplias, no medidas globalmente** | JUnit/Mockito/MockMvc, H2 y PostgreSQL, concurrencia, rollback, smoke Compose | Sin porcentaje de cobertura actual verificado; Selenium separado/manual y flujos felices no equivalen a pruebas de estrés, pentesting o accesibilidad |
| CI/CD | **Sólida para PRs funcionales** | Siete checks verdes en la integración del PR #32; pipelines H2, PostgreSQL y Compose | Sonar es manual. `tests.yml` es manual y el job Selenium usa `${{ inputs.test }}` sin entrada declarada en `workflow_dispatch`: revisar su ejecución |
| Infraestructura | **Reproducible en entorno local** | Compose, volúmenes persistentes, Docker multi-stage, usuario 10001, credenciales mediante entorno | No existe despliegue público controlado, backups, gestión de secretos/rotación, observabilidad operativa ni restauración probada |
| Documentación y portfolio | **Buena** | README actualizado, capturas, guías PostgreSQL, auditoría y créditos | No hay licencia explícita en la raíz; aclarar el alcance de reutilización según el repositorio académico original |

## Hallazgos priorizados para la tercera fase

### P0 — Corregir antes de publicar una instancia accesible a terceros

1. **Edición administrativa de tickets** — `TicketController.saveTicket(@ModelAttribute Ticket)` reenvía a `TicketService.save(ticket)`, que ejecuta `ticketRepository.save(ticket)` sin normalizar campos. Aunque la ruta requiere ADMIN, es una superficie de *mass assignment* y de modificación de atributos sensibles (estado, usuario, sesión, precio, QR, butaca). Rediseñar como comando/DTO con lista permitida e invariantes del servicio; cubrir solicitudes HTTP manipuladas. **No se ha demostrado explotación por un usuario no administrador.**
2. **Integridad de sesiones después de generar entradas** — `SessionManagementService` impide cambiar la sala cuando existen entradas, pero aún permite cambiar película, precio y horario. Decidir la política de negocio para inventario generado y entradas pagadas, y probarla; no asumir que cambiar esos campos es siempre inválido.
3. **Identidad de butacas a nivel de base de datos** — en `V1__initial_schema.sql` hay índices por sesión, pero no restricción única de `(session_id,fila,seat)`. Validar registros existentes, definir migración **V2** y prueba de duplicados/concurrencia. No modificar la migración V1 ya aplicada.

### P1 — Valor visible para calidad y operabilidad

4. **Cobertura y controles automáticos** — medir cobertura real con JaCoCo en un build reproducible; fijar objetivo **por código nuevo** y reporte visible, no inventar un porcentaje del repositorio. Valorar revisión de dependencias y análisis estático en PR sin romper el flujo de contribución.
5. **Selenium / workflow manual** — comprobar el parámetro `inputs.test` usado en `.github/workflows/tests.yml` y declarar la entrada u optar por una selección fija. Ejecutar y documentar una suite E2E estable, no solo contar clases de tests.
6. **Validación de formularios y errores** — completar validaciones Bean Validation en los DTOs restantes; comprobar UX de 400/409 y errores de modelo. Evitar excepciones genéricas como mecanismo de validación cuando el formulario puede mostrar errores de campo.

### P2 — Acabado del portfolio y preparación de operaciones

7. **Licencia y atribución** — verificar derechos del proyecto colaborativo antes de elegir licencia o publicar una reutilización; actualmente no existe un archivo `LICENSE` en la raíz.
8. **Observabilidad, backups y despliegue** — si se aspira a demo online, definir entorno separado, gestión de secretos, métricas, logs sin datos sensibles, copias de seguridad y prueba de recuperación; mantener la advertencia de que el Compose actual es local.
9. **Limpieza del repositorio** — revisar ramas remotas fusionadas remanentes (`docs/security-hardening-audit`, `fix/atomic-session-ticket-creation`, `fix/review-post-authorization`, `fix/safe-image-uploads`) y borrar solo las que realmente ya no tengan trabajo pendiente, previa confirmación.

## Propuesta de ejecución

1. **Discovery de reglas de ticket/sesión (P0)**: fijar los campos editables y los que deben ser inmutables con ventas. Primero tests negativos; después PR independientes de código.
2. **Integridad SQL (P0)**: auditar butacas y preparar V2 con estrategia de datos existentes, tests PostgreSQL y rollback de despliegue.
3. **Quality gate y E2E (P1)**: obtener línea base medible de cobertura, corregir workflow Selenium, evaluar Sonar/dependencias.
4. **Portfolio y operaciones (P2)**: completar la atribución, preparar demostración grabada o despliegue público seguro si se desea.

## Condiciones para dar un asunto por cerrado

- Issue/ticket con hipótesis y criterio de aceptación verificable.
- Un PR por cambio independiente, sin fusionar hasta checks verdes.
- Tests que reproduzcan el riesgo donde sea posible y no dependan de una transacción de test para demostrar atomicidad real.
- Cambios de BD mediante nueva migración Flyway compatible con datos persistidos.
- Distinguir siempre **observación estática**, **prueba reproducida** y **riesgo residual**.

## Conclusión

Para entrevistas y portfolio, **el siguiente mayor retorno no es añadir más pantallas**, sino demostrar reglas de dominio sólidas, esquema de datos protegido y calidad reproducible. Esta valoración es **cualitativa** y **no es una certificación de seguridad ni una puntuación objetiva**.
