# Auditoría de consolidación técnica — Only Film

**Fecha:** 2026-10-10  
**Referencia:** `main` tras la integración del PR #12 (`0a7e03a0`)  
**Tipo de revisión:** inspección estática de código, configuración, CI y documentación; **no equivale a pentest ni verifica una explotación activa**.

## Resumen ejecutivo

Only Film dispone ya de control de permisos en tickets, pruebas de concurrencia, PostgreSQL 17, Flyway, Docker Compose, persistencia de uploads y carga demo opcional. La revisión descubre varios riesgos de autorización, semántica HTTP y configuración que conviene resolver antes de publicar el servicio en Internet.

## Hallazgos priorizados

| Orden | Ticket | Severidad | Evidencia revisada | Acción sugerida |
|---|---|---|---|---|
| 1 | #13 | Crítica, pendiente reproducción | `ReviewController.saveReview` obtiene el autor de la reseña existente y hace `save` sin verificar propietario ni rol; el GET de edición sí comprueba ambos | Escribir test de regresión para POST no autorizado y corregir comprobaciones; proteger contra mass assignment |
| 2 | #14 | Alta | `@GetMapping` modifica/elimina películas, salas, sesiones, usuarios y reseñas | Cambiar a operaciones POST con CSRF y actualizar formularios |
| 3 | #15 | Alta | `FileService` usa la extensión del nombre enviado y `WebConfig` sirve `/uploads/**` | Validar formato binario real y bloquear contenido activo |
| 4 | #16 | Alta | `application.properties` incluye consola H2, `show-sql=true` y contraseña estática para usuario por defecto de Spring | Aislar perfiles de desarrollo y eliminar valores inseguros |
| 5 | #17 | Media | `README.md` aún anuncia H2 persistente, clonado de `certidevs/g1_testing`, métricas antiguas y PostgreSQL como futuro | Actualizar arquitectura, comandos, contribuciones y métricas verificables |

### Observaciones adicionales para el refinamiento

- `SecurityConfig` protege correctamente los POST de creación de películas y los tickets; **no elimina la necesidad de autorización dentro del caso de uso** (p. ej., edición de reseñas ajenas).
- En `ReviewController.reviewDetail`, aceptar cualquier `returnUrl` que comience por `/` podría aceptar `//otro-dominio` si el valor termina en redirección o enlace externo. Validar su utilización real en Thymeleaf antes de abrir una incidencia separada.
- `MovieController.createMovie`, `SessionController.saveSession` y otros métodos usan `@ModelAttribute` directamente sobre entidades JPA. Revisar explícitamente qué propiedades puede controlar el cliente y considerar DTOs por operación.
- `UserService.update` asigna `role` y `active` según `userForm`, por lo que hay que probar que **todas** las rutas que lo invocan fijan esos valores a partir del usuario autenticado y que no existe escalada de privilegios por formulario.
- El `DataInitializer` demo usa contraseñas de ejemplo conocidas; el perfil `demo` solo debe habilitarse en entornos locales aislados.
- Verificar credenciales, límites y protección de datos personales antes de plantear un despliegue público.

## Secuencia de trabajo

1. **Bloqueo de seguridad (#13):** test de regresión negativo primero, corrección y validación de la operación autorizada.
2. **Mutaciones HTTP (#14):** revisar todas las plantillas y rutas, migrar con tests CSRF.
3. **Uploads (#15):** validar imagen binaria, restricciones y cobertura HTTP.
4. **Configuración (#16):** endurecimiento de perfiles y dependencias del entorno.
5. **README (#17):** actualización contra implementación y resultados CI reales.

## Política de validación

Cada ticket se abordará en una rama independiente creada desde `main`, con PR propio. No se aceptará merge hasta tener tests de seguridad, H2, PostgreSQL y Compose en verde cuando correspondan. La auditoría no modifica código ejecutable ni supone que los hallazgos ya estén reproducidos dinámicamente.
