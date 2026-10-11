# P2.31 — Consola H2 de desarrollo: activación explícita y loopback

La consola H2 no se habilita con el perfil `dev` por sí solo. En `application-dev.properties`:
- `server.address=127.0.0.1` limita la escucha HTTP del proceso a la interfaz local **cuando esa propiedad efectiva no se sobrescribe**.
- `spring.h2.console.enabled=${ONLYFILM_H2_CONSOLE_ENABLED:false}` exige activación expresa.
- `SecurityConfig` solo omite CSRF para `/h2-console/**` si `dev` está activo, `postgres` y `prod` no lo están, la consola está habilitada y la dirección efectiva es `127.0.0.1`. Los demás endpoints mantienen protección CSRF normal.
- El perfil `postgres` fuerza la consola deshabilitada en su archivo de propiedades.

Para trabajar localmente con la consola: activar el perfil `dev` y establecer la variable de entorno `ONLYFILM_H2_CONSOLE_ENABLED=true`. Se presupone que la aplicación se ejecuta en la máquina de desarrollo, sin publicar un túnel/proxy inverso que exponga la consola.

**Limitaciones:** un operador puede sobrescribir propiedades de configuración, publicar puertos mediante proxy/túnel, o añadir perfiles de forma inapropiada. Comprobar siempre la configuración efectiva y exposición de red del despliegue; el nombre de perfil `dev` no certifica que la máquina sea segura. La consola H2 es una herramienta exclusivamente local y nunca debe habilitarse en servidores públicos.

Pruebas: `H2ConsoleCsrfProfileTest` comprueba el opt-in local y rechaza ausencia de opt-in, consola deshabilitada, binding público, y perfiles de producción/PostgreSQL. `EnvironmentHardeningTest` comprueba las propiedades comprometidas. El estado de `java:S4502` en SonarCloud no se deduce de estos cambios.
