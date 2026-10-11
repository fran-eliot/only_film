# P2.34 — Contrato de títulos Thymeleaf

Relacionado con [#139](https://github.com/fran-eliot/only_film/issues/139) y auditoría [#49](https://github.com/fran-eliot/only_film/issues/49).

## Hallazgo comprobado

Las plantillas de páginas analizadas usan la inclusión Thymeleaf `<head th:replace="~{layout/head :: head('...')}"></head>`; el fragmento compartido `layout/head.html` contiene `<title th:text="${title}">Only Film</title>`.

Al inspeccionar el HTML fuente por separado, un analizador estático puede informar `Web:PageWithoutTitleCheck` en una página que obtiene su título del fragmento en tiempo de renderizado. No debe añadirse un segundo `<title>` sin comprobar el HTML renderizado. Las plantillas `layout/` son fragmentos reutilizables, no documentos navegables por sí mismos.

## Regresión

`ThymeleafHeadTitleContractTest` confirma que el fragmento compartido define un título parametrizado y que todas las plantillas HTML fuera de `layout/` referencian ese fragmento con un argumento de título. El workflow H2 ejecuta explícitamente la clase.

## Límites

Esta prueba es estructural y no valida el DOM de cada ruta ni confirma el estado del Quality Gate. Es necesario un inventario actualizado de incidencias de SonarCloud en `main` antes de clasificar cada alerta como resuelta, falsa positiva o pendiente.
