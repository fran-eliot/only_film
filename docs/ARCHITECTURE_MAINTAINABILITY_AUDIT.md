# Revisión de arquitectura y mantenibilidad — Only Film

**Fecha:** 2026-10-10  
**Referencia:** rama `main` tras PR #23 (`b6cf52c9`)  
**Método:** inspección estática del código Java, DTOs, controladores, servicios y sus dependencias. Los riesgos no reproducidos deben verificarse con tests antes de describirse como defectos explotables.

## Estado de partida

El proyecto ya dispone de medidas de autorización en tickets y reseñas, formularios POST con CSRF, validación de imágenes, perfiles seguros, PostgreSQL/Flyway y pruebas automatizadas. La siguiente fase debe **conservar las garantías existentes** y evitar una reescritura arquitectónica amplia.

La arquitectura MVC nominal sigue controladores → servicios → repositorios, pero no todos los casos de uso aplican esa frontera: `MovieController`, `RoomController` y `SessionController` escriben directamente mediante repositorios.

## Hallazgos priorizados

| Orden | Issue | Hallazgo observado | Riesgo / pregunta para validar |
|---|---|---|---|
| 1 | [#26](https://github.com/fran-eliot/only_film/issues/26) | `UserService.update` recibe una entidad `User` y asigna `role`/`active` desde el formulario; `saveProfile` intenta sanear dichos valores previamente | Separación insuficiente entre actualizaciones propias y administrativas; verificar invariantes ante formularios manipulados y cambios de rol |
| 2 | [#24](https://github.com/fran-eliot/only_film/issues/24) | Películas, salas y sesiones enlazan `@ModelAttribute` directamente a entidades JPA y en ocasiones se llama a `repository.save` sin lista explícita de campos editables | Mass assignment, cambios accidentales de asociaciones o pérdida de campos no expuestos al formulario |
| 3 | [#25](https://github.com/fran-eliot/only_film/issues/25) | `SessionController.saveSession` guarda la sesión antes de `ticketService.generarTickets`; no se observa transacción única del caso de uso | Sesión persistida sin entradas si la generación falla; validar con tests de rollback |
| 4 | [#27](https://github.com/fran-eliot/only_film/issues/27) | Tratamiento dispar de ausencias, acceso no autorizado, validaciones y excepciones entre controladores | Respuestas y mensajes no coherentes, posibles errores internos expuestos o convertidos en 500 |

### Evidencia y límites

- `src/main/java/com/demo/controller/MovieController.java`: `createMovie(@ModelAttribute Movie)` guarda directamente un `Movie` construido desde el formulario.
- `src/main/java/com/demo/controller/RoomController.java`: `guardar(@Valid @ModelAttribute Room)` invoca `roomRepository.save(room)`.
- `src/main/java/com/demo/controller/SessionController.java`: `saveSession(@ModelAttribute Session)` hace `sessionRepository.save` seguido de `ticketService.generarTickets` solo en la creación.
- `src/main/java/com/demo/service/UserService.java`: `update(User, Long)` aplica rol, estado, imagen, email y contraseña a la entidad persistente.
- `src/main/java/com/demo/controller/UserController.java`: `saveProfile` fija id, rol y estado a partir del principal antes de llamar a `UserService.update`, una defensa existente que debe mantenerse.
- El proyecto ya usa DTOs (`RegisterForm`, `UserStatsDTO`) y un servicio de checkout transaccional. El cambio recomendado es **incremental**, no sustituir todo el diseño.

## Plan de implementación sugerido

1. **#26 — Usuarios / privilegios**: tests de escenarios manipulados y autoadministración; DTOs/comandos diferenciados según rol y reglas en el servicio.
2. **#24 — Entradas MVC**: empezar por un único formulario (películas) y ampliar a salas/sesiones si mejora la seguridad sin coste excesivo.
3. **#25 — Casos de uso transaccionales**: trasladar creación de sesión y generación de entradas a un servicio atómico, con tests de rollback real.
4. **#27 — Errores MVC**: acordar política explícita y mejorar endpoint a endpoint tras estabilizar los casos de uso.

## Criterios comunes de salida

- Issue y rama independientes; PR hacia `main`, nunca cambios directos.
- Test negativo que demuestre el comportamiento problemático cuando sea posible.
- Conservar control de roles y CSRF, checkout y datos existentes.
- Verificar CI H2, PostgreSQL/Flyway, Compose y regresión aplicable antes de merge.
- No alterar esquemas Flyway ni datos persistidos sin migración planificada.
