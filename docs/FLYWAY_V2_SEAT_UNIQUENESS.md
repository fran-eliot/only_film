# Flyway V2 — unicidad de butacas por sesión

## Regla

Una butaca identificada por `(session_id, fila, seat)` no puede estar duplicada dentro de la misma sesión, independientemente de su estado de venta. La misma fila/asiento puede existir en **sesiones distintas**.

La migración `V2__unique_ticket_seats.sql` conserva V1 sin modificaciones y añade la restricción `uq_tickets_session_row_seat`. PostgreSQL permite valores `NULL` repetidos en una restricción `UNIQUE`: esta migración protege las posiciones completas, **no** modifica ni rechaza explícitamente registros históricos incompletos.

## Auditoría obligatoria de datos existentes

Antes de desplegar en un PostgreSQL con datos reales, ejecutar:

```sql
SELECT session_id, fila, seat, COUNT(*) AS duplicates, ARRAY_AGG(id ORDER BY id) AS ticket_ids
FROM tickets
WHERE session_id IS NOT NULL AND fila IS NOT NULL AND seat IS NOT NULL
GROUP BY session_id, fila, seat
HAVING COUNT(*) > 1;
```

La consulta debe devolver **cero filas**. Si devuelve colisiones, investigar individualmente qué entradas están pagadas y resolverlas **con aprobación funcional**, sin borrar registros o alterar referencias automáticamente. V2 incluye una verificación defensiva que falla la migración antes del `ALTER TABLE` si encuentra duplicados.

Para auditar datos incompletos (fuera del alcance de la unicidad):

```sql
SELECT id, session_id, fila, seat, status
FROM tickets
WHERE session_id IS NULL OR fila IS NULL OR seat IS NULL;
```

## Validación y despliegue

- PostgreSQL CI: confirma la aplicación exitosa de V1 y V2 antes y después de reiniciar aplicación/base de datos.
- `PostgresPersistenceIntegrationTest`: comprueba inserción duplicada rechazada y reutilización válida del mismo asiento entre sesiones diferentes.
- Una base previamente migrada con V1 recibe únicamente V2; **no editar ni reejecutar V1**.
- Ante fallos de despliegue, restaurar desde backup verificado según el procedimiento del entorno. No se ofrece una migración destructiva automática para «resolver» duplicados.
