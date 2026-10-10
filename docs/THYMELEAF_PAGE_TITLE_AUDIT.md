# P2.5f — Títulos HTML en Thymeleaf y SonarQube

## Contexto

La auditoría SonarQube (#49) detecta 27 avisos `Web:PageWithoutTitleCheck` en plantillas Thymeleaf. Las páginas examinadas (cartelera, formulario de reseñas y error 403) utilizan `<head th:replace="~{layout/head :: head('...')}"></head>`. El fragmento compartido `layout/head.html` define `<title th:text="${title}">Only Film</title>`.

## Criterio de verificación

La presencia de la etiqueta `<title>` en el archivo HTML fuente no demuestra por sí sola cómo se entrega la página. Verificar la **respuesta HTML renderizada**, después del procesamiento de Thymeleaf; un título correcto y específico en la respuesta significa que no hay defecto funcional para esa ruta.

Se incorporan comprobaciones MockMvc en la cartelera (`/movies`) y el formulario de reseñas (`/reviews/new`). Mantener las pruebas en CI para detectar regresiones del fragmento compartido.

## Tratamiento de avisos

Cuando un aviso afecte a una plantilla que sustituya el encabezado con `th:replace`, comprobar primero su respuesta final. Si existe el título esperado, documentar la evidencia y considerar el aviso como posible falso positivo del análisis estático. No insertar `<title>` duplicados en cada plantilla solo para silenciar la regla; si se decide clasificar avisos en SonarQube, utilizar la opción de resolución apropiada y justificarla.

Los demás avisos aún requieren comprobación por página, en particular los fragmentos independientes y las páginas con rutas especiales. Esta documentación no equivale a resolver automáticamente los 27 avisos.
