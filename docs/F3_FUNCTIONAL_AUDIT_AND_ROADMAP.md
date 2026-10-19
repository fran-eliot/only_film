# F3.001 — Auditoría funcional y hoja de ruta de evolución

**Base revisada:** `main` en `df63353564485eb6d2d60c982032e5d3c4facbf2`, octubre de 2026.  
**Método:** inspección estática de README, entidades, DTOs, controladores, servicios, repositorios, migraciones y auditorías previas. No constituye una validación visual completa ni una auditoría de producción.

## Objetivo de la fase 3

Pasar de la corrección intensiva de incidencias SonarCloud a mejoras funcionales con valor real para un cine y a nuevas competencias técnicas demostrables en el portfolio. Mantener el análisis SonarCloud manual y la disciplina de issues, PR y CI.

## Capacidades verificadas

- **Cartelera y sesiones:** consulta de películas y horarios, administración de películas, salas y proyecciones.
- **Usuarios y roles:** registro, autenticación, perfiles, administración de usuarios y protección con Spring Security.
- **Entradas y checkout:** selección/compra simulada, QR, historial, bloqueo transaccional y protección de concurrencia. La aplicación no integra una pasarela de cobro real.
- **Reseñas:** publicación y moderación con reglas de autorización.
- **Persistencia:** H2 local; PostgreSQL con Flyway y Docker Compose; pruebas H2/PostgreSQL y Selenium en CI.
- **Indicadores existentes:** `UserStatsDTO` y estadísticas por usuario; no se ha verificado un dashboard de negocio global de ocupación/recaudación.

## Hallazgos históricos revisados: no duplicar trabajo

| Riesgo documentado en auditoría anterior | Estado actual comprobado | Consecuencia |
| --- | --- | --- |
| `POST /tickets/save` enlaza directamente la entidad Ticket | El controlador recibe `@Valid TicketAdminForm`; `TicketService.updateAdminDiscount` limita la edición al descuento de una entrada libre y usa bloqueo | El hallazgo antiguo no debe reabrirse sin una regresión nueva |
| Cambios esenciales de sesiones con ventas | `SessionManagementService.validateAndSynchronizeEdit` bloquea cambios esenciales cuando hay entradas vendidas y sincroniza precio de inventario no vendido | Mantener pruebas; estudiar reglas nuevas solo con casos de negocio concretos |
| Duplicación de butacas en PostgreSQL | Existe `V2__unique_ticket_seats.sql` con `UNIQUE(session_id, fila, seat)` | No crear otra migración equivalente |
| CSRF y parámetro `null` de request cache | Quedan dos hallazgos SonarCloud identificados, con pruebas y documentación parcial | Gestionar como deuda de seguridad y verificación operativa, no como bloqueo artificial del desarrollo |

## Roadmap propuesto

| Orden | Iniciativa | Valor de negocio y alcance inicial | Validación |
| --- | --- | --- | --- |
| F3.002 | **Descubrimiento del dashboard de cine** | Definir KPIs: entradas pagadas, ingresos simulados, ocupación por sesión, películas más vendidas y filtros temporales; inventariar campos y semántica de descuentos/cancelaciones | Contratos, definiciones de métricas, criterios de aceptación y mocks; sin calcular ingresos financieros reales a partir del checkout simulado |
| F3.003 | **Consultas agregadas de dashboard** | DTOs de lectura, servicio y consultas JPA/PostgreSQL con filtros por fechas; sin cargar todas las entradas en memoria | Tests de agregados, rangos de fecha, entradas libres/pagadas, datos vacíos y acceso ADMIN |
| F3.004 | **Dashboard MVC con Thymeleaf** | Indicadores y gráficos accesibles/responsivos, enlazados a las consultas de F3.003 | MockMvc, Selenium y revisión visual; no cambiar el checkout |
| F3.005 | **API REST de consulta** | Endpoints `/api/v1/admin/metrics` para métricas autorizadas y documentación OpenAPI | Autorización, contratos JSON y tests de integración; reutilizar servicios, evitar duplicar negocio |
| F3.006 | **Observabilidad** | Actuator/Micrometer y métricas de salud/rendimiento; evaluar Prometheus/Grafana solo si hay escenario operativo | No exponer endpoints sensibles públicamente; pruebas de configuración |
| F3.007 | **Caché justificada por medición** | Valorar Redis solo tras identificar consultas costosas y política de invalidación | Benchmarks y pruebas; no introducir Redis como dependencia ornamental |
| F3.008 | **Operabilidad y demo controlada** | Datos ficticios, documentación, accesibilidad, despliegue acotado, secretos y copias de seguridad | Checklist de seguridad y prueba de recuperación antes de exponer el servicio |

## Decisiones y límites

1. **Empezar por el dashboard** como ampliación visible que también ejercita Spring Data, SQL, DTOs, Thymeleaf, pruebas y potencialmente API REST.
2. **No tratar el pago simulado como ingreso real**: definir el término «recaudación simulada» en los KPIs.
3. **No añadir Redis, colas o microservicios de manera anticipada**; exigir una necesidad o medición.
4. **No mezclar funcionalidades con refactorizaciones masivas**: PR pequeñas por incremento vertical probado.
5. **SonarCloud manual**, dos hallazgos remanentes pendientes de tratamiento justificado.
6. **No afirmar que una función falta** por no haberla encontrado en esta revisión estática; confirmar en el discovery correspondiente.

## Próxima entrega

Abrir F3.002 como discovery de métricas: revisar modelo `Ticket`, `Session`, `Movie`, repositorios y semántica de estados/importe; definir métricas, permisos, filtros, casos límite y bosquejo de interfaz. Solo después comenzar código funcional.

## Protocolo de ejecución

Issue → rama → implementación y tests → PR → cinco workflows/siete jobs verdes → merge verificado → limpieza de rama. Documentación vinculada a la issue cuando cambien contratos, reglas de negocio o endpoints.
