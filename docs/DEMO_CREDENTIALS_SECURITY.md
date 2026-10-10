# Cuentas de demostración y credenciales

## Cambios de seguridad

El inicializador `DataInitializer` crea cuentas de ejemplo cuando su configuración de perfiles lo permite. Las contraseñas literales conocidas incluidas originalmente en código eran reutilizables por terceros y provocaban alertas `java:S6437` de SonarQube Cloud.

Desde P2.2, cada cuenta de demostración recibe **una contraseña aleatoria distinta** cuando no se establece configuración externa. Las contraseñas generadas no se muestran en logs ni se guardan en el repositorio. Los datos de películas, usuarios de ejemplo y relaciones permanecen disponibles para las demostraciones, pero **no se podrá iniciar sesión con esas cuentas usando contraseñas conocidas** sin configurar explícitamente un secreto local.

Para una **demo local controlada**, establecer en el entorno:

```bash
export ONLYFILM_DEMO_PASSWORD='<una-contraseña-local-única>'
```

En PowerShell:

```powershell
$env:ONLYFILM_DEMO_PASSWORD = '<una-contraseña-local-única>'
```

Spring Boot resuelve esta variable como `onlyfilm.demo.password`. Configurarla hace que **todas las cuentas sembradas compartan la misma contraseña** para facilitar una demostración local, por lo que **no debe utilizarse en servicios públicos, entornos reales o compartidos**. Preferir cuentas registradas normalmente en esos casos.

No cambia los perfiles del inicializador, la base de datos ni las contraseñas de usuarios ya persistidos. En modo PostgreSQL, el sembrado de demostración continúa sujeto a su autorización explícita y a que la base esté vacía.

## Rotación necesaria cuando corresponda

Retirar literales del código y un comentario con una huella bcrypt **no los borra del historial Git** ni modifica credenciales ya guardadas en bases de datos. Si alguna contraseña de demostración histórica fue reutilizada en un entorno real, debe cambiarse allí. La huella bcrypt que SonarQube detectó en `UserService` era un comentario, no una credencial utilizada en el flujo de autenticación.

## Pruebas

`EnvironmentHardeningTest` verifica que, sin configuración, se generan valores distintos e impredecibles entre invocaciones y que la propiedad externa se respeta cuando se configura. La suite de regresión existente continúa protegiendo autenticación y flujos de negocio.
