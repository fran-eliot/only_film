# P2.29 — Reevaluación de SonarCloud tras estabilizar Selenium

Seguimiento de [#125](https://github.com/fran-eliot/only_film/issues/125) y auditoría general [#49](https://github.com/fran-eliot/only_film/issues/49). Referencia de código: `main` tras PR #124 (merge `c6b33c0f4de56cc424f0edce44da4495b9243807`).

## Fuentes y límites de la evaluación

- **Última exportación completa registrada, histórica (tras PR #88, 11/10/2026):** 168 incidencias abiertas, 137 code smells, 30 bugs, una vulnerabilidad, deuda total 813 min (13 h 33 min). Estos números **no describen el estado actual** de `main`.
- La issue #49 documenta una línea base anterior de 182 incidencias y la reducción neta a 168 en ese momento. No extrapolar esa evolución tras P2.12–P2.28 sin un nuevo export.
- En los logs del PR #124, SonarCloud informa `ANALYSIS SUCCESSFUL`; esto demuestra la ejecución del análisis del **PR**, **no** el estado del Quality Gate, cobertura ni recuento de issues de `main`.
- Las consultas públicas a SonarCloud para métricas e issues no fueron accesibles en esta revisión. No se dispone de un conector autorizado para leer el proyecto. **Estado actual de Quality Gate, cobertura, vulnerabilidades y Security Hotspots: sin confirmar.**
- La suite CI del PR #124 obtuvo 7 jobs exitosos, 13 Selenium sin reintentos y 199 tests de regresión H2. Esto es evidencia de CI, no sustituto de las métricas SonarCloud.

## Riesgos y tareas a contrastar con el export actualizado

| Prioridad provisional | Hallazgo histórico (export tras PR #88) | Acción basada en evidencia pendiente |
| --- | --- | --- |
| P0 de revisión | `java:S4502` (1 vulnerabilidad en SecurityConfig) | Revisar si continúa abierta, condiciones efectivas del perfil local H2, rutas y configuración en despliegue. No desactivar CSRF global. |
| P1 | `Web:PageWithoutTitleCheck` (27) | Verificar cada archivo/fragmento Thymeleaf y la página HTML renderizada: no añadir títulos a fragmentos sin documento independiente. |
| P1/P2 | `java:S8688` (27) | Evaluar uso horario de cada ubicación antes de cambiar clases y conversiones temporales. |
| P2 | `java:S117` (11), `java:S7467` (13), `java:S125` (16) | Clasificar reglas y archivos actuales y ejecutar correcciones locales solo si aportan valor; comprobar si S125 bajó con limpiezas anteriores. |
| P2 | `java:S6206` (1 en FileService) | Contrastar con S6218, ya corregida antes, para evitar oscilaciones entre record/clase. |
| P2 | Accesibilidad `Web:S5255`, `Web:S6807`, `css:S7924` | Inspeccionar impacto real en páginas y estilos. |

`java:S2187`, `java:S3776` y algunos `java:S1192` aparecían corregidos o se abordaron después del histórico; confirmar su estado **solo** con el nuevo export.

## Datos necesarios para una auditoría cuantitativa actualizada

1. Abrir [SonarCloud — incidencias de main](https://sonarcloud.io/project/issues?id=fran-eliot_only_film&branch=main) con acceso al proyecto. Anotar fecha/hora del análisis de `main` y commit asociado.
2. Exportar **todas las páginas** de incidencias abiertas desde la API autorizada, por ejemplo `/api/issues/search?componentKeys=fran-eliot_only_film&branch=main&resolved=false&ps=100&p=1`; continuar `p=2,3,...` hasta cubrir `paging.total`. Guardar JSON sin publicar tokens.
3. Registrar Quality Gate mediante la pantalla del proyecto (o `/api/qualitygates/project_status?projectKey=fran-eliot_only_film&branch=main` si el acceso lo permite), junto con condiciones que fallan y la cobertura visible.
4. Exportar/revisar Security Hotspots (incluidos sus estados) por separado: el inventario de issues no los sustituye.
5. Comparar issues por **clave** con el histórico de #49. Contar resueltas y nuevas y agrupar por regla, severidad, tipo, archivo y deuda; diferenciar cambios de severidad y de alcance.

## Criterios para siguientes fases P2.30+

- **Primero:** vulnerabilidades confirmadas y configuraciones de seguridad con riesgo efectivo.
- **Segundo:** bugs reproducibles e incumplimientos del Quality Gate que estén realmente presentes en `main`.
- **Tercero:** accesibilidad y deuda de mantenibilidad de bajo riesgo, en PR pequeños con tests.
- No crear tickets con números o líneas actuales sin export; no afirmar resolución por `ANALYSIS SUCCESSFUL`.
- Mantener **#49 abierta** hasta verificar Quality Gate, completar clasificación y cerrar el inventario de hallazgos presentes.

## Resultado de P2.29

Documento de reevaluación y protocolo reproducible, **sin nueva medición cuantitativa** por falta de acceso a la API. La comparación histórica→actual permanece pendiente; no se modificó código de producción.
