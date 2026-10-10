package com.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment) throws Exception {

        // En Spring Security 6+ HttpSessionRequestCache solo devuelve la petición guardada
        // si la URL contiene el parámetro "continue". Al ponerlo a null desactivamos ese check
        // y recuperamos el comportamiento clásico: tras login vuelves a donde ibas.
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName(null);
        http.requestCache(cache -> cache.requestCache(requestCache));

        // H2 console uses frames and POST without CSRF; allow it ONLY in explicit local dev.
        // Do not bypass CSRF or relax frame protections in standard/PostgreSQL execution.
        if (environment.acceptsProfiles(Profiles.of("dev"))
                && !environment.acceptsProfiles(Profiles.of("postgres"))) {
            http.csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"));
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        //protección de rutas
        http.authorizeHttpRequests(auth -> auth
                // 1. Recursos Públicos y Autenticación
                .requestMatchers("/", "/register", "/login",  "/error", "/css/**", "/webjars/**", "/images/**", "/uploads/**", "/.well-known/**").permitAll()

                // 2. Control de Películas (MovieController)
                // IMPORTANTE: las rutas específicas ("/movies/new", etc.) deben declararse
                // ANTES que "/movies/{id}", porque Spring Security va mirando en orden
                // y se queda con el PRIMERO que coincida. Como {id} es una variable,
                // "/movies/new" también matchea con "/movies/{id}" (id="new"), así que si
                // "/movies/{id}" va primero con permitAll(), "/movies/new" quedaría público
                // sin pasar nunca por la regla hasRole("ADMIN").
                .requestMatchers("/movies/new", "/movies/edit/**", "/movies/deactivate/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/movies", "/movies/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/movies").hasRole("ADMIN")

                // 3. Control de Salas (RoomController)
                .requestMatchers(HttpMethod.GET, "/salas", "/salas/{id}").hasRole("ADMIN")
                .requestMatchers("/salas/new", "/salas/edit/**", "/salas/deactivate/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/salas").hasRole("ADMIN")

                // 4. Control de Sesiones / Proyecciones (SessionController - ¡Faltaba!)
                .requestMatchers(HttpMethod.GET, "/sessions", "/sessions/{id}").permitAll() // Público para que vean la cartelera/horarios
                .requestMatchers("/sessions/new", "/sessions/edit/**", "/sessions/delete/**", "/sessions/deactivate/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/sessions").hasRole("ADMIN")

                // 5. Control de Reseñas (ReviewController)
                .requestMatchers(HttpMethod.GET, "/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/reviews/new").hasAnyRole("ADMIN", "USER") // debe ir ANTES que /reviews/{id} !!
                .requestMatchers(HttpMethod.GET, "/reviews/{id}").permitAll()
                .requestMatchers("/reviews/edit/**").hasAnyRole("ADMIN", "USER")
                .requestMatchers(HttpMethod.POST, "/reviews").hasAnyRole("ADMIN", "USER")
                .requestMatchers("/reviews/disable/**", "/reviews/delete/**").hasRole("ADMIN")

                // 6. Control de Tickets / Compras (TicketController y Órdenes)
                .requestMatchers(HttpMethod.GET, "/tickets", "/tickets/{id}").authenticated() // Ver todos los tickets es de Admin
                .requestMatchers("/tickets/edit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/tickets/save").hasRole("ADMIN")
                .requestMatchers("/tickets/buy/**").authenticated() // Comprar requiere estar logueado

                // (paso 6.5) panel de usuarios para admins
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 7. Cualquier otra ruta no especificada requerirá estar autenticado
                .anyRequest().authenticated()
        );
        //http.exceptionHandling(exception -> exception.accessDeniedPage("/movies"));

        http.exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, e) ->
                        response.sendError(403))
        );
        // Config del formulario de login
        http.formLogin(form -> form
                // pagina personalizada de login (en vez de la login por defecto de Spring)
                .loginPage("/login")

                // Qué hacer cuando el login es correcto ->
                .successHandler(savedRequestAwareAuthenticationSuccessHandler())
                // eso anterior es un "handler" que decide a dónde redirigir tras login

                // Permite que cualquiera pueda acceder al login sin estar autenticado
                .permitAll()
        );

        // contruir la cadena de filtros de seguridad (vamos, la configuración final obligatoria)
        return http.build();
    }
    @Bean
    public SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler() {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();

        // la URL por defecto si el usuario entra
        // directamente a /login sin venir de otra página protegida
        //y no venía de ninguna página protegida
        // -> después del login lo envío a /movies
        successHandler.setDefaultTargetUrl("/movies");

        // esto es opcional, parámetro en la URL para forzar redirección
        successHandler.setTargetUrlParameter("redirectTo");

        return successHandler;
    }
}