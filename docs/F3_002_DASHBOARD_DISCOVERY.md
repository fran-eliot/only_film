# F3.002 — Discovery del dashboard administrativo

Base: main f0082a600c975a347bd09278f3293122db3e5208. Esta es una especificación, no una implementación.

## Modelo verificado
- Ticket: status (LIBRE, INICIADO, PAGADO, CANCELADO), buyDateTime, price, discount, snackPrice, foodPrice, session y user.
- Session: startTime, price, movie, room, active. Room: capacity; Movie: title.
- Al completar checkout, TicketService establece PAGADO, fecha efectiva de compra, price = Session.price - discount y snackPrice.
- El inventario LIBRE también tiene buyDateTime inicializado. Nunca contar filas por fecha sin filtrar PAGADO.
- El pago es simulado; la recaudación no representa cobros bancarios.

## KPIs y origen temporal
1. Entradas vendidas: contar PAGADO por Ticket.buyDateTime.
2. Recaudación simulada de entradas: sumar Ticket.price de PAGADO (descuento ya aplicado, nunca restarlo otra vez).
3. Snacks simulados: sumar COALESCE(Ticket.snackPrice,0) de PAGADO.
4. Total simulado: entradas + snacks; no añadir foodPrice hasta estudiar su semántica.
5. Ocupación de una sesión: tickets PAGADO / tickets de inventario de esa sesión; N/A cuando no exista inventario.
6. Películas más vendidas: contar PAGADO agrupadas por Session.movie.id, ordenar de forma determinista.

Los estados LIBRE, INICIADO y CANCELADO no computan como ventas. El estado CANCELADO puede haber tenido una compra previa: sin un historial de pagos y reembolsos no pueden calcularse ingresos netos reales.

## Filtros
- Ventas por intervalo [desde, hasta) de fecha de compra, convertido desde días locales Europe/Madrid.
- Ocupación por intervalo [desde, hasta) de Session.startTime, separado de fecha de compra.
- Opcionales movieId y roomId. Predeterminado sugerido: últimos 30 días.
- Las ventas históricas de sesiones hoy desactivadas siguen siendo visibles.
- Sin datos: conteos y sumas cero; ocupación sin inventario N/A.

## Acceso y arquitectura
- GET /admin/dashboard (ROLE_ADMIN); verificar matcher SecurityConfig en la implementación.
- DashboardMetricsService de solo lectura, DTOs/proyecciones agregadas, consultas JPQL/SQL en base de datos; nunca cargar todo Ticket a memoria.
- Thymeleaf: tarjetas KPI, serie por fechas, ranking y ocupación. Filtros accesibles, estados vacíos y diseño responsive.
- No exponer datos de compradores ni códigos QR. No incorporar Redis sin justificación medible.

## Criterios para F3.003
- ADMIN autorizado; usuario sin rol denegado.
- PAGADO incluido y otros estados excluidos.
- Precio de sesión 10, descuento 2: ingreso entradas 8, no 6.
- Snacks agregados sin duplicar tickets.
- Cuatro ventas de diez plazas generan 40% de ocupación.
- Rango temporal semiabierto y fechas de ventas diferenciadas de proyecciones.
- Datos vacíos, nulos heredados, sesiones inactivas y ranking estable probados.
- Pruebas H2 y PostgreSQL, sin cambiar el comportamiento de checkout.

## Decisiones por validar
- Uso real y semántica de foodPrice.
- Cancelaciones y posibles reembolsos no disponen de ledger transaccional.
- Definición de ocupación global (sesiones realizadas, futuras o todas): empezar por ocupación por sesión.
- Revisión de seguridad y representación de fechas para futura API REST.

Siguiente incremento: F3.003, servicios y consultas de métricas con tests.