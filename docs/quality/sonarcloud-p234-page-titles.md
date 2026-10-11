# P2.34 — Clasificación preliminar de Web:PageWithoutTitleCheck

Relacionado con #137 y la auditoría general #49.

## Evidencia del repositorio
- `src/main/resources/templates/layout/head.html` declara `<head th:fragment="head(title)">` y el elemento `<title th:text="${title}">Only Film</title>`.
- La vista `movies/movie-list.html` incorpora `head('Cartelera — Only Film')` mediante `th:replace`. El `<title>` real aparece cuando Thymeleaf procesa la plantilla; no se puede inferir su ausencia solamente por inspeccionar el archivo fuente.
- `layout/navbar.html` y `layout/footer.html` son fragmentos destinados a incrustarse en páginas, no documentos HTML independientes.
- `RenderedPageTitleTest` comprueba por MockMvc que `GET /movies` devuelve el título renderizado esperado. Es una prueba representativa, no una auditoría exhaustiva de todas las vistas.

## SonarCloud: límite de evidencia
`Web:PageWithoutTitleCheck` tenía 27 hallazgos en una exportación **histórica** registrada tras PR #88. No hay inventario confirmado para `main` actual. Un hallazgo del analizador sobre una plantilla Thymeleaf puede requerir revisión manual del HTML procesado; no resolver añadiendo títulos repetidos en plantillas o fragmentos por defecto.

Para clasificar cada alerta vigente: obtener export completo de issues en `main`, identificar archivo/línea, distinguir vistas completas de fragmentos y verificar el HTML renderizado de las rutas relevantes. Documentar en SonarCloud las justificaciones cuando proceda, sin marcarlas como falso positivo sin evidencia.

Quality Gate y estado de `java:S4502`: pendientes de acceso al panel o export de SonarCloud.
