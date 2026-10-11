# P2.5f — Títulos HTML en Thymeleaf y SonarQube

## Contexto

La auditoría SonarQube (#49) detecta 27 avisos `Web:PageWithoutTitleCheck` en plantillas Thymeleaf. Las páginas examinadas (cartelera, formulario de reseñas y error 403) utilizan `<head th:replace="~{layout/head :: head('...')}"></head>`. El fragmento compartido `layout/head.html` define `<title th:text="${title}">Only Film</title>`.

## Criterio de verificación

La presencia de la etiqueta `<title>` en el archivo HTML fuente no demuestra por sí sola cómo se entrega la página. Verificar la **respuesta HTML renderizada**, después del procesamiento de Thymeleaf; un título correcto y específico en la respuesta significa que no hay defecto funcional para esa ruta.

Se incorporan comprobaciones MockMvc en la cartelera (`/movies`) y el formulario de reseñas (`/reviews/new`). Mantener las pruebas en CI para detectar regresiones del fragmento compartido.

## Tratamiento de avisos

Cuando un aviso afecte a una plantilla que sustituya el encabezado con `th:replace`, comprobar primero su respuesta final. Si existe el título esperado, documentar la evidencia y considerar el aviso como posible falso positivo del análisis estático. No insertar `<title>` duplicados en cada plantilla solo para silenciar la regla; si se decide clasificar avisos en SonarQube, utilizar la opción de resolución apropiada y justificarla.

Los demás avisos aún requieren comprobación por página, en particular los fragmentos independientes y las páginas con rutas especiales. Esta documentación no equivale a resolver automáticamente los 27 avisos.

## P2.9 — Clasificación y cobertura incremental (11/10/2026)

El inventario exportado de SonarCloud mantiene **27** incidencias `Web:PageWithoutTitleCheck` en `main`, incluyendo los fragmentos `layout/navbar.html` y `layout/footer.html`. Estos dos archivos definen fragmentos reutilizables `th:fragment` y no se sirven como páginas HTML completas: no añadirles títulos ficticios. La plantilla `layout/head.html` define el fragmento `head(title)` y el `<title th:text="\${title}">Only Film</title>` compartido.

### Evidencia disponible

- **HTML renderizado verificado previamente (P2.5f):** `/movies` (cartelera) y `/reviews/new` (formulario de reseña), mediante MockMvc.
- **Contrato de plantilla fuente añadido en P2.9:** `ThymeleafTitleTemplateContractTest` comprueba el título dinámico en `layout/head.html` y el uso de `th:replace` en cuatro representantes: `auth/login.html`, `error/404.html`, `tickets/ticket-checkout.html` y `users/user-list.html`.
- **Fragmentos no navegables como página:** `layout/navbar.html` y `layout/footer.html`, identificados por sus respectivos `th:fragment`.
- **Resto de plantillas:** pendientes de validación funcional individual. La existencia de un fragmento compartido en las muestras no demuestra que todas las rutas de la aplicación rindan correctamente sus títulos.

### Decisión

Conservar el mecanismo común `th:replace` y no insertar etiquetas `<title>` redundantes con el único fin de silenciar una regla estática. SonarCloud puede mantener los avisos mientras no se clasifiquen manualmente con evidencias. No marcar como resueltos en bloque; revisar las rutas concretas y los HTML renderizados en los casos restantes.
