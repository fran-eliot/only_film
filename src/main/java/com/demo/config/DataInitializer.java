package com.demo.config;

import com.demo.model.*;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.model.enums.ScreenType;
import com.demo.repository.*;
import com.demo.service.TicketService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
@AllArgsConstructor
@Profile("!test & (!postgres | demo)")
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Europe/Madrid");
    private static final String LANGUAGE_DUBBED = "doblada";
    private static final String LANGUAGE_ORIGINAL = "VO";
    private static final String LANGUAGE_ORIGINAL_SUBTITLED = "VOSE";

    private static final String GENRE_ROMANCE = "Romance";
    private static final String GENRE_COMEDY = "Comedia";


    private RoomRepository roomRepo;
    private SessionRepository sessionRepo;
    private MovieRepository movieRepo;
    private TicketRepository ticketRepo;
    private UserRepository userRepo;
    private ReviewRepository reviewRepo;
    private TicketService ticketService;
    private PasswordEncoder passwordEncoder;
    private Environment environment;

    /**
     * Sin configuración explícita, las cuentas demo reciben contraseñas aleatorias
     * que no se publican ni quedan disponibles para iniciar sesión.
     */
    String demoPassword() {
        String configured = environment.getProperty("onlyfilm.demo.password");
        return configured != null && !configured.isBlank()
                ? configured
                : java.util.UUID.randomUUID().toString();
    }

    // PostgreSQL demo data is opt-in and must never overwrite an existing database.
    private boolean skipNonEmptyPostgresDatabase() {
        if (!java.util.Arrays.asList(environment.getActiveProfiles()).contains("postgres")) {
            return false;
        }
        boolean empty = movieRepo.count() == 0 && roomRepo.count() == 0
                && sessionRepo.count() == 0 && ticketRepo.count() == 0
                && userRepo.count() == 0 && reviewRepo.count() == 0;
        if (!empty) {
            log.info("-> Demo seeding skipped: database is not empty.");
            return true;
        }
        log.warn("-> WARNING: creating demonstration accounts with known passwords. Local use only.");
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) throws Exception {
        if (skipNonEmptyPostgresDatabase()) {
            return;
        }
        log.info("-> Inicializando datos de prueba...");

        //Datos de prueba de las PELICULAS
        Movie m0 = movieRepo.save(Movie.builder().title("Top Gun: Ídolos del aire - (40 Aniversario)").director("Tony Scott").active(true).releaseYear(1986)
                .sinopsis("La escuela de pilotos de élite de Estados Unidos prepara a los nuevos conductores de los F-14. En sus aulas, talento y ego chocarán entre dos jóvenes pilotos, Maverick y Iceman.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BZWYyZWY2NDctYjFlOC00ZjNjLWExYWEtZDA3MDllYWZmYTk5XkEyXkFqcGc@._V1_.jpg").durationMinutes(108).genre("Accion").build());

        Movie m1 = movieRepo.save(Movie.builder().title("Las ovejas detectives").director("Kyle Balda").active(true).releaseYear(2026)
                .sinopsis("En esta ingeniosa y novedosa película de misterio, George (Hugh Jackman) es un pastor que cada noche lee novelas policíacas a sus queridas ovejas, dando por sentado que no pueden entenderlas. Pero cuando un misterioso incidente altera la vida en la granja, las ovejas se dan cuenta de que deben convertirse en detectives. A medida que siguen las pistas e investigan a los sospechosos humanos, demuestran que incluso las ovejas pueden ser brillantes resolviendo crímenes.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BYTk0ZDI5NTAtYWQ5MS00MmI2LWIxMTktOWFkNTEwYzg0OWE4XkEyXkFqcGc@._V1_.jpg").durationMinutes(109).genre("Animación").build());

        Movie m2 = movieRepo.save(Movie.builder().title("The Mandalorian y Grogu").director("Jon Favreau").durationMinutes(132).active(true).releaseYear(2026)
                .sinopsis("El malvado Imperio ha caído y los señores de la guerra imperiales siguen dispersos por toda la galaxia. Mientras la incipiente Nueva República trabaja para proteger todo por lo que luchó la Rebelión, ha reclutado la ayuda del legendario cazarrecompensas mandaloriano Din Djarin (Pedro Pascal) y su joven aprendiz Grogu.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BZmI1NzdjYTUtM2Y1MC00MDJmLWFlMmMtZDQzNGY1Y2E4NjA0XkEyXkFqcGc@._V1_.jpg").genre("Ciencia ficción").build());

        Movie m3 = movieRepo.save(Movie.builder().title("El drama").director("Kristoffer Borgli").durationMinutes(105).active(true).releaseYear(2026)
                .sinopsis("Una pareja, en los días previos a su boda, se enfrenta a una crisis cuando unas inesperadas revelaciones desbaratan lo que uno de ellos creía saber sobre el otro.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BMTcwOGZhYmItYTg3ZS00NjUwLWJiMmQtMjU5YjEwYWY0NmNkXkEyXkFqcGc@._V1_.jpg").genre(GENRE_ROMANCE).build());

        Movie m4 = movieRepo.save(Movie.builder().title("Proyecto salvación").director("Phil Lord").durationMinutes(156).active(true).releaseYear(2026)
                .sinopsis("El profesor de ciencias Ryland Grace (Ryan Gosling) se despierta en una nave espacial a años luz de casa sin recordar quién es ni cómo ha llegado hasta allí. A medida que recupera la memoria, empieza a descubrir su misión: resolver el enigma de la misteriosa sustancia que provoca la extinción del sol. Deberá recurrir a sus conocimientos científicos y a sus ideas poco ortodoxas para salvar todo lo que hay en la Tierra de la extinción... pero una amistad inesperada significa que quizá no tenga que hacerlo solo.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BYTJmNThhZGMtMmJiNC00MmQ5LWIzYzEtMmM4ZGM5NWY3NzRhXkEyXkFqcGc@._V1_.jpg").genre("Ciencia ficción").build());

        Movie m5 = movieRepo.save(Movie.builder().title("La odisea").director("Christopher Nolan").durationMinutes(112).active(true).releaseYear(2026)
                .sinopsis("El legendario Odiseo enfrenta peligros épicos en su viaje de regreso a casa tras la Guerra de Troya. Se topa con criaturas míticas y dioses caprichosos, superando obstáculos increíbles antes de reunirse con su amada Penélope.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BMjVjZGJiMWItNTkxNC00ODFiLTlkZDYtOGFmZmE2ODkxZmJlXkEyXkFqcGc@._V1_.jpg").genre("Accion").build());


        Movie m6 = movieRepo.save(Movie.builder().title("Todo lo que nunca fuimos").director("Jorge Alonso").durationMinutes(120).active(true).releaseYear(2026)
                .sinopsis("Tras perder a sus padres, Leah, una joven pintora, vive sumida en depresión. Su hermano le pide a Axel que cuide de ella mientras está fuera, sin saber que entre ellos surgirá un amor que podría cambiarlo todo.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BYmI2NzVhZjMtOTgzOC00ZmMxLTkzMjctNjA4YTllN2Y0NjBhXkEyXkFqcGc@._V1_.jpg").genre(GENRE_ROMANCE).build());

        Movie m7 = movieRepo.save(Movie.builder().title("Toy Story 5").director("McKenna Harris").durationMinutes(90).active(true).releaseYear(2026)
                .sinopsis("Cuando Woody, Buzz, Jessie y la pandilla se encuentran con un recién llegado de alta tecnología, sus aventuras dan un giro inesperado mientras compiten por demostrar que los juguetes más valiosos de la vida no se limitan a códigos y chips.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BMDQwZTM3NWUtNGI4Yi00YWRmLWExYWItNDNmNWVmYWEyZGUxXkEyXkFqcGc@._V1_.jpg").genre(GENRE_COMEDY).build());

        Movie m8 = movieRepo.save(Movie.builder().title("Playa de lobos").director("Javier Veiga").durationMinutes(100).active(true).releaseYear(2026)
                .sinopsis("Manu trabaja en un chiringuito. Klaus no suelta la última hamaca. Lo que parece un encuentro entre opuestos se vuelve sospechoso cuando Manu duda de Klaus. La tensión aumenta.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BNjlkZDdiYzMtZmU3OS00ODk2LTk5YzgtMTg0NzMxNTZhNmY0XkEyXkFqcGc@._V1_.jpg").genre(GENRE_COMEDY).build());

        Movie m9 = movieRepo.save(Movie.builder().title("El ser querido").director("Rodrigo Sorogoyen").durationMinutes(135).active(true).releaseYear(2026)
                .sinopsis("Un aclamado director se reencuentra con su distanciada hija, una actriz sin éxito, para rodar juntos una película, enfrentándose a su tensa relación y a asuntos del pasado sin resolver que ninguno de los dos quiere abordar directamente.")
                .imageUrl("https://m.media-amazon.com/images/M/MV5BMmI3OTI5Y2ItZTJlMS00MjIwLTg5ZGUtOWFiZmUzOTE1NDFjXkEyXkFqcGc@._V1_.jpg").genre(GENRE_ROMANCE).build());


        Movie m10 = movieRepo.save(Movie.builder().active(true)
                .title(" The Super Mario Galaxy Movie").director("Aaron Horvath").durationMinutes(98).sinopsis("").genre("Adventure").releaseYear(2026)
                .imageUrl("https://m.media-amazon.com/images/M/MV5BMGIzMGI4OWYtNzdkMy00MjJlLWJiMDMtZjY1Y2UwMzQ0YzY3XkEyXkFqcGc@._V1_.jpg")
                .sinopsis("La película tiene lugar después de los acontecimientos de la primera, en la que dos hermanos, Mario y Luigi, y la princesa Peach emprenden una aventura hasta los confines del espacio y a través de la galaxia. Secuela de Super Mario Bros La película, basada en la saga Mario Galaxy.").build());

        Movie m11 = movieRepo.save(Movie.builder().active(true)
                .title(" The Devil Wears Prada 2").director("David Frankel").durationMinutes(119).genre(GENRE_COMEDY).releaseYear(2026)
                .imageUrl("https://m.media-amazon.com/images/M/MV5BYjZhNTE0ZTktYThlZC00OWUwLTlhMDItNzlkMjJkOGJhZTc5XkEyXkFqcGc@._V1_.jpg")
                .sinopsis("Casi veinte años después de interpretar a los icónicos personajes de Miranda, Andy, Emily y Nigel, Meryl Streep, Anne Hathaway, Emily Blunt y Stanley Tucci regresan a las calles de Nueva York y a las oficinas de la revista Runway en la secuela del fenómeno de 2006 que definió a toda una generación.").build());


        //Datos de prueba de las salas (Room)
        // Precios por tipo de sala (aplicados en sesiones):
        //   D4X  (Sala 1) → 13,00 €
        //   IMAX (Sala 2) → 14,00 €
        //   D3   (Sala 4) → 11,00 €
        // ══════════════════════════════════════════════════════════════════════
        var room1 = Room.builder().name("Sala 1").active(true).screenType(ScreenType.D4X).capacity(100).build();
        var room2 = Room.builder().name("Sala 2").active(true).screenType(ScreenType.IMAX).capacity(150).build();
        var room3 = Room.builder().name("Sala 3").active(true).screenType(ScreenType.D3).capacity(80).build();
        var room4 = Room.builder().name("Sala 4").screenType(ScreenType.D3).active(true).capacity(60).build();
        var room5 = Room.builder().name("Sala 5").screenType(ScreenType.STANDARD).active(true).capacity(100).build();
        var room6 = Room.builder().name("Sala 6").screenType(ScreenType.STANDARD).active(true).capacity(125).build();
        roomRepo.saveAll(List.of(room1, room2, room3, room4, room5, room6));

        // ==========================================
        // Datos de prueba de los USUARIOS (Users)
        // ==========================================

        userRepo.save(User.builder()
                .firstName("Admin").lastName("Cinema").username("admin")
                .email("admin@cinema.com").role(Role.ROLE_ADMIN).active(true).imageUrl("/uploads/lobo.png")
                .password(passwordEncoder.encode(demoPassword())).build());

        userRepo.save(User.builder()
                .firstName("Admin2").lastName("Cinema").username("admin2")
                .email("admin2@cinema.com").role(Role.ROLE_ADMIN).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var user = userRepo.save(User.builder()
                .firstName("Alejandro").lastName("Mendoza").username("user")
                .email("alex_m98@gmail.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var user2 = userRepo.save(User.builder()
                .firstName("Maria").lastName("Fernandez").username("user2")
                .email("maria.fernandez@gmail.com").role(Role.ROLE_USER).active(false)
                .password(passwordEncoder.encode(demoPassword())).build());

        // — Usuarios nuevos —
        var carlosR = userRepo.save(User.builder()
                .firstName("Carlos").lastName("Ruiz").username("carlos_r")
                .email("carlos.ruiz@outlook.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var lauraS = userRepo.save(User.builder()
                .firstName("Laura").lastName("Sánchez").username("laura_s")
                .email("laurasanchez91@gmail.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var pabloG = userRepo.save(User.builder()
                .firstName("Pablo").lastName("García").username("pablo_g")
                .email("pablo.garcia@icloud.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var elenaM = userRepo.save(User.builder()
                .firstName("Elena").lastName("Morales").username("elena_m")
                .email("elena.morales@hotmail.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var javierT = userRepo.save(User.builder()
                .firstName("Javier").lastName("Torres").username("javier_t")
                .email("jtorres_cine@gmail.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        var sofiaL = userRepo.save(User.builder()
                .firstName("Sofía").lastName("López").username("sofia_l")
                .email("sofia.lopez@gmail.com").role(Role.ROLE_USER).active(true)
                .password(passwordEncoder.encode(demoPassword())).build());

        // ==========================================
        // SESIONES PASADAS (para historial de tickets de user1)
        //
        //  pasada1     → hace 7 días → La odisea        (4DX,  13€) → user1 compró 2
        //  pasada2     → hace 5 días → Proyecto salv.   (IMAX, 14€) → user1 compró 1
        //  pasada3     → hace 3 días → The Mandalorian  (3D,   11€) → user1 compró 3
        //  sesAgotada  → hace 1 día  → Toy Story 5      (IMAX, 14€) → todos PAGADO
        // ==========================================
        LocalDate hoy = LocalDate.now(CINEMA_ZONE);

        Session pasada1 = sessionRepo.save(Session.builder()
                .movie(m5).room(room1).price(13.00)
                .language(LANGUAGE_DUBBED).adMinutes(15)
                .startTime(hoy.minusDays(7).atTime(19, 0)).build());

        Session pasada2 = sessionRepo.save(Session.builder()
                .movie(m4).room(room2).price(14.00)
                .language(LANGUAGE_ORIGINAL).adMinutes(20)
                .startTime(hoy.minusDays(5).atTime(21, 0)).build());

        Session pasada3 = sessionRepo.save(Session.builder()
                .movie(m2).room(room4).price(11.00)
                .language(LANGUAGE_ORIGINAL_SUBTITLED).adMinutes(15)
                .startTime(hoy.minusDays(3).atTime(18, 30)).build());

        Session sesAgotada = sessionRepo.save(Session.builder()
                .movie(m7).room(room2).price(14.00)
                .language(LANGUAGE_DUBBED).adMinutes(10)
                .startTime(hoy.minusDays(1).atTime(16, 0)).build());

        // SESIONES FUTURAS — 8 días vista, 5 salas activas (room3 inactiva)
        //
        // Precio por sala:  4DX(1)=13€  IMAX(2)=14€  3D(4)=11€  STANDARD(5)=10€  STANDARD(6)=9€
        //
        // Sala 1 · 4DX : [finde 11:30 Mandalorian] · 16:30 La odisea · 19:00 Mandalorian · 21:30 Proyecto
        // Sala 2 · IMAX: [finde 11:00 Toy Story]   · 16:00 Super Mario · 18:30 La odisea · 21:00 Top Gun
        // Sala 4 · 3D  : 11:00 Ovejas(par)/Todo(impar) · 16:30 Devil Wears · 19:00 Playa · 21:15 El drama
        // Sala 5 · STD : 10:30 Toy Story · 16:00 Super Mario · 18:30 Las ovejas · 21:00 El ser querido
        // Sala 6 · STD : 10:00 El drama  · 16:30 Proyecto    · 19:00 Top Gun   · 21:30 Playa de lobos
        List<Session> sesionesFuturas = new ArrayList<>();

        // Referencias a las sesiones de HOY para asignar tickets a usuarios
        Session todayOdisea4dx = null, todayMandalorian4dx = null, todayProyecto4dx = null;
        Session todayMarioImax = null, todayOdiseaImax = null, todayTopgunImax = null;
        Session todayTopgunStd = null, todayPlayaStd = null;

        for (int dia = 0; dia <= 7; dia++) {
            LocalDate fecha = hoy.plusDays(dia);
            int dow = fecha.getDayOfWeek().getValue(); // 1=Lun … 7=Dom
            boolean esFinDeSemana = (dow == 6 || dow == 7);

            // ── Sala 1 · 4DX · 13€ ────────────────────────────────────────────
            if (esFinDeSemana) {
                sesionesFuturas.add(sessionRepo.save(Session.builder()
                        .movie(m2).room(room1).price(13.00).language(LANGUAGE_DUBBED).adMinutes(20)
                        .startTime(fecha.atTime(11, 30)).build()));
            }
            Session r1a = sessionRepo.save(Session.builder()
                    .movie(m5).room(room1).price(13.00).language(LANGUAGE_DUBBED).adMinutes(15)
                    .startTime(fecha.atTime(16, 30)).build());
            sesionesFuturas.add(r1a);

            Session r1b = sessionRepo.save(Session.builder()
                    .movie(m2).room(room1).price(13.00).language(LANGUAGE_ORIGINAL).adMinutes(20)
                    .startTime(fecha.atTime(19, 0)).build());
            sesionesFuturas.add(r1b);

            Session r1c = sessionRepo.save(Session.builder()
                    .movie(m4).room(room1).price(13.00).language(LANGUAGE_ORIGINAL_SUBTITLED).adMinutes(15)
                    .startTime(fecha.atTime(21, 30)).build());
            sesionesFuturas.add(r1c);

            // ── Sala 2 · IMAX · 14€ ───────────────────────────────────────────
            if (esFinDeSemana) {
                sesionesFuturas.add(sessionRepo.save(Session.builder()
                        .movie(m7).room(room2).price(14.00).language(LANGUAGE_DUBBED).adMinutes(10)
                        .startTime(fecha.atTime(11, 0)).build()));
            }
            Session r2a = sessionRepo.save(Session.builder()
                    .movie(m10).room(room2).price(14.00).language(LANGUAGE_DUBBED).adMinutes(15)
                    .startTime(fecha.atTime(16, 0)).build());
            sesionesFuturas.add(r2a);

            Session r2b = sessionRepo.save(Session.builder()
                    .movie(m5).room(room2).price(14.00).language(LANGUAGE_ORIGINAL).adMinutes(15)
                    .startTime(fecha.atTime(18, 30)).build());
            sesionesFuturas.add(r2b);

            Session r2c = sessionRepo.save(Session.builder()
                    .movie(m0).room(room2).price(14.00).language(LANGUAGE_ORIGINAL_SUBTITLED).adMinutes(15)
                    .startTime(fecha.atTime(21, 0)).build());
            sesionesFuturas.add(r2c);

            // ── Sala 4 · 3D · 11€ ─────────────────────────────────────────────
            if (dia % 2 == 0) {
                sesionesFuturas.add(sessionRepo.save(Session.builder()
                        .movie(m1).room(room4).price(11.00).language(LANGUAGE_DUBBED).adMinutes(10)
                        .startTime(fecha.atTime(11, 0)).build()));
            } else {
                sesionesFuturas.add(sessionRepo.save(Session.builder()
                        .movie(m6).room(room4).price(11.00).language(LANGUAGE_ORIGINAL).adMinutes(13)
                        .startTime(fecha.atTime(11, 0)).build()));
            }
            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m11).room(room4).price(11.00).language(LANGUAGE_ORIGINAL).adMinutes(15)
                    .startTime(fecha.atTime(16, 30)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m8).room(room4).price(11.00).language(LANGUAGE_ORIGINAL).adMinutes(12)
                    .startTime(fecha.atTime(19, 0)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m3).room(room4).price(11.00).language(LANGUAGE_ORIGINAL_SUBTITLED).adMinutes(12)
                    .startTime(fecha.atTime(21, 15)).build()));

            // ── Sala 5 · STANDARD · 10€ ───────────────────────────────────────
            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m7).room(room5).price(10.00).language(LANGUAGE_DUBBED).adMinutes(10)
                    .startTime(fecha.atTime(10, 30)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m10).room(room5).price(10.00).language(LANGUAGE_DUBBED).adMinutes(15)
                    .startTime(fecha.atTime(16, 0)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m1).room(room5).price(10.00).language(LANGUAGE_DUBBED).adMinutes(10)
                    .startTime(fecha.atTime(18, 30)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m9).room(room5).price(10.00).language(LANGUAGE_ORIGINAL).adMinutes(18)
                    .startTime(fecha.atTime(21, 0)).build()));

            // ── Sala 6 · STANDARD · 9€ ────────────────────────────────────────
            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m3).room(room6).price(9.00).language(LANGUAGE_ORIGINAL).adMinutes(12)
                    .startTime(fecha.atTime(10, 0)).build()));

            sesionesFuturas.add(sessionRepo.save(Session.builder()
                    .movie(m4).room(room6).price(9.00).language(LANGUAGE_DUBBED).adMinutes(15)
                    .startTime(fecha.atTime(16, 30)).build()));

            Session r6c = sessionRepo.save(Session.builder()
                    .movie(m0).room(room6).price(9.00).language(LANGUAGE_DUBBED).adMinutes(15)
                    .startTime(fecha.atTime(19, 0)).build());
            sesionesFuturas.add(r6c);

            Session r6d = sessionRepo.save(Session.builder()
                    .movie(m8).room(room6).price(9.00).language(LANGUAGE_DUBBED).adMinutes(12)
                    .startTime(fecha.atTime(21, 30)).build());
            sesionesFuturas.add(r6d);

            // Guardar referencias del día 0 para los tickets de usuarios de prueba
            if (dia == 0) {
                todayOdisea4dx      = r1a;
                todayMandalorian4dx = r1b;
                todayProyecto4dx    = r1c;
                todayMarioImax     = r2a;
                todayOdiseaImax    = r2b;
                todayTopgunImax    = r2c;
                todayTopgunStd     = r6c;
                todayPlayaStd      = r6d;
            }
        }

        // GENERAR TICKETS para todas las sesiones
        ticketService.generarTickets(pasada1);
        ticketService.generarTickets(pasada2);
        ticketService.generarTickets(pasada3);
        ticketService.generarTickets(sesAgotada);

        for (Session s : sesionesFuturas) {
            ticketService.generarTickets(s);
        }

        seedPastPurchases(pasada1, pasada2, pasada3, sesAgotada, user);
        seedTodayPurchases(
                new Session[]{todayOdisea4dx, todayMandalorian4dx, todayProyecto4dx,
                        todayMarioImax, todayOdiseaImax, todayTopgunImax, todayTopgunStd, todayPlayaStd},
                new User[]{user, user2, carlosR, lauraS, pabloG, elenaM, javierT, sofiaL});

        // REVIEWS
        // — La odisea (m5) —
        reviewRepo.save(Review.builder()
                .title("Nolan lo vuelve a hacer")
                .description("Llevaba meses esperando esta película y no me ha decepcionado. " +
                        "Nolan consigue que la epopeya de Odiseo se sienta completamente actual. " +
                        "La fotografía es brutal, especialmente en IMAX. De las mejores del año sin duda.")
                .rating(5).movie(m5).user(carlosR)
                .creationDate(hoy.minusDays(1).atTime(22, 14)).build());

        reviewRepo.save(Review.builder()
                .title("Espectacular en IMAX, obligatoria")
                .description("Si vas a verla, hazlo en IMAX o no te molestes. El sonido envolvente " +
                        "y la escala visual son una experiencia que no se puede describir con palabras. " +
                        "La actuación del protagonista es increíble. Puntuación mínima: 5 estrellas.")
                .rating(5).movie(m5).user(lauraS)
                .creationDate(hoy.minusDays(2).atTime(18, 30)).build());

        reviewRepo.save(Review.builder()
                .title("Demasiado larga, pero impresionante")
                .description("No me malinterpretéis, la película es una obra de arte. Pero con 172 " +
                        "minutos hay momentos en los que se hace pesada, especialmente en el segundo acto. " +
                        "Aun así, Nolan demuestra por qué sigue siendo el mejor director de su generación.")
                .rating(4).movie(m5).user(javierT)
                .creationDate(hoy.minusDays(4).atTime(21, 5)).build());

        // — The Mandalorian y Grogu (m2) —
        reviewRepo.save(Review.builder()
                .title("Grogu para siempre")
                .description("Soy muy fan de la serie y la película me ha encantado. Grogu sigue " +
                        "siendo adorable y la relación con Din Djarin tiene momentos que se te quedan " +
                        "grabados. Los efectos visuales están a otro nivel comparado con la serie. Muy recomendable.")
                .rating(4).movie(m2).user(elenaM)
                .creationDate(hoy.minusDays(3).atTime(20, 45)).build());

        reviewRepo.save(Review.builder()
                .title("Si no has visto la serie, espera")
                .description("Para los que somos fans del universo Star Wars la película es un regalo. " +
                        "Pero creo que sin haber visto las temporadas de la serie te puedes perder mucho " +
                        "contexto. Aun así la historia principal se sigue bien y los efectos son espectaculares.")
                .rating(3).movie(m2).user(sofiaL)
                .creationDate(hoy.minusDays(6).atTime(19, 20)).build());

        // — Toy Story 5 (m7) —
        reviewRepo.save(Review.builder()
                .title("Muy emotiva y divertida")
                .description("Fui con mis hijos pensando que sería una película más del montón y " +
                        "me sorprendió muchísimo. Los guionistas han conseguido que Woody y Buzz " +
                        "vuelvan a sentirse frescos. Acabé llorando en el cine, no lo voy a negar.")
                .rating(4).movie(m7).user(user2)
                .creationDate(hoy.minusDays(2).atTime(17, 10)).build());

        reviewRepo.save(Review.builder()
                .title("Pixar en estado puro")
                .description("Esperaba poco después de Toy Story 4 y me ha dejado con la boca abierta. " +
                        "La animación ha dado un salto de calidad brutal. Los momentos de humor funcionan " +
                        "perfectamente y el mensaje final es precioso. Para toda la familia.")
                .rating(5).movie(m7).user(pabloG)
                .creationDate(hoy.minusDays(5).atTime(16, 30)).build());

        // — Proyecto salvación (m4) —
        reviewRepo.save(Review.builder()
                .title("Entretenida aunque algo lenta")
                .description("La premisa es muy interesante y Ryan Gosling está fantástico, " +
                        "pero hay momentos en el segundo acto donde la película pierde ritmo. " +
                        "La parte científica está muy bien explicada y la amistad que surge es entrañable. " +
                        "Recomendada para fans de la ciencia ficción dura.")
                .rating(3).movie(m4).user(carlosR)
                .creationDate(hoy.minusDays(7).atTime(23, 0)).build());

        reviewRepo.save(Review.builder()
                .title("Una de las mejores de ciencia ficción en años")
                .description("Hacía tiempo que no veía una peli de ciencia ficción que me enganchara " +
                        "tanto desde el principio. La historia de amistad es el corazón de la película " +
                        "y lo llevan de maravilla. El final me dejó sin palabras. Hay que verla.")
                .rating(5).movie(m4).user(sofiaL)
                .creationDate(hoy.minusDays(9).atTime(20, 15)).build());

        // — Las ovejas detectives (m1) —
        reviewRepo.save(Review.builder()
                .title("Me ha encantado")
                .description("La fui a ver con mi sobrina de 7 años y las dos salimos encantadas. " +
                        "Los gags visuales son ingeniosos y hay guiños para adultos que no se notan " +
                        "a primera vista. La oveja detective protagonista es un personaje genial. " +
                        "Hugh Jackman está muy gracioso en el doblaje original.")
                .rating(5).movie(m1).user(elenaM)
                .creationDate(hoy.minusDays(3).atTime(12, 45)).build());

        reviewRepo.save(Review.builder()
                .title("Entretenida pero predecible")
                .description("La animación es preciosa y tiene momentos muy divertidos, " +
                        "pero el argumento sigue demasiado el guión clásico de misterio sin sorprender. " +
                        "Para niños es perfecta, los adultos pueden aburrirse un poco a mitad. " +
                        "Buen rato en familia de todas formas.")
                .rating(3).movie(m1).user(javierT)
                .creationDate(hoy.minusDays(8).atTime(18, 0)).build());

        // — El drama (m3) —
        reviewRepo.save(Review.builder()
                .title("No es para mí, demasiado lenta")
                .description("Entiendo que busca un estilo contemplativo y que mucha gente lo valora, " +
                        "pero a mí personalmente me costó mucho mantener la atención. " +
                        "Las actuaciones son buenas, eso sí. Creo que es una película muy de festivales, " +
                        "no tanto para el cine comercial.")
                .rating(2).movie(m3).user(user)
                .creationDate(hoy.minusDays(5).atTime(22, 30)).build());

        reviewRepo.save(Review.builder()
                .title("Brutalmente honesta sobre las relaciones")
                .description("De las pocas películas de los últimos años que retrata de verdad cómo " +
                        "funcionan las relaciones de pareja sin caer en tópicos. Incómoda en algunos " +
                        "momentos, pero precisamente eso es lo que la hace grande. Para verla en pareja " +
                        "y luego hablar largo y tendido.")
                .rating(4).movie(m3).user(lauraS)
                .creationDate(hoy.minusDays(10).atTime(21, 0)).build());

        // — Super Mario Galaxy (m10) —
        reviewRepo.save(Review.builder()
                .title("Muy buena para toda la familia")
                .description("La primera película de Mario me encantó y esta supera en muchos aspectos " +
                        "a su predecesora. El viaje por la galaxia es visualmente increíble. " +
                        "Los fans de los videojuegos encontrarán muchísimas referencias. " +
                        "Mi hijo salió del cine queriendo jugar al Nintendo Switch de inmediato.")
                .rating(5).movie(m10).user(pabloG)
                .creationDate(hoy.minusDays(1).atTime(17, 45)).build());

        reviewRepo.save(Review.builder()
                .title("Divertida pero inferior a la primera")
                .description("No llega al nivel de la primera película, que tenía más corazón y " +
                        "mejores momentos emotivos. Esta apuesta más por el espectáculo visual " +
                        "que por la historia. Aun así es entretenida y los niños van a flipar. " +
                        "El diseño de los mundos de la galaxia es una pasada.")
                .rating(3).movie(m10).user(user2)
                .creationDate(hoy.minusDays(6).atTime(16, 0)).build());

        // — The Devil Wears Prada 2 (m11) —
        reviewRepo.save(Review.builder()
                .title("Meryl Streep insuperable")
                .description("Casi veinte años después y Meryl Streep sigue siendo la reina. Miranda " +
                        "Priestly tiene momentos en esta secuela que superan a los de la primera. " +
                        "Anne Hathaway también está muy bien, aunque su arco es algo predecible. " +
                        "Una secuela que no traiciona el espíritu del original.")
                .rating(4).movie(m11).user(sofiaL)
                .creationDate(hoy.minusDays(4).atTime(19, 30)).build());

        reviewRepo.save(Review.builder()
                .title("Nostalgia pura, aunque algo forzada")
                .description("La vi con mis amigas y lo pasamos genial, pero siendo objetiva la historia " +
                        "está un poco cogida con pinzas. Parece que han buscado reunir al elenco " +
                        "por nostalgia más que por necesidad narrativa. El estilo y los looks son " +
                        "espectaculares como siempre. Perfecta para una noche de chicas.")
                .rating(3).movie(m11).user(elenaM)
                .creationDate(hoy.minusDays(11).atTime(21, 30)).build());

        // — Top Gun (m0) —
        reviewRepo.save(Review.builder()
                .title("Un clásico que sigue funcionando")
                .description("Ver Top Gun en pantalla grande sigue siendo una experiencia increíble " +
                        "incluso 40 años después. La banda sonora te pone los pelos de punta y las " +
                        "escenas de vuelo siguen siendo espectaculares. Ideal para los que la vivieron " +
                        "en su época y para los que la descubren ahora.")
                .rating(5).movie(m0).user(javierT)
                .creationDate(hoy.minusDays(2).atTime(20, 0)).build());

        // — To_do lo que nunca fuimos (m6) —
        //SIN REVIEWWS


        // — Playa de lobos (m8) —
        reviewRepo.save(Review.builder()
                .title("La comedia española del año")
                .description("Hacía tiempo que no me reía tanto en el cine. La dinámica entre " +
                        "los dos protagonistas es perfecta y los diálogos están muy bien escritos. " +
                        "Además tiene un giro que no me vi venir para nada. Javier Veiga " +
                        "demuestra que el cine de comedia español está muy vivo.")
                .rating(5).movie(m8).user(user)
                .creationDate(hoy.minusDays(5).atTime(21, 15)).build());

        // — El ser querido (m9) —
        reviewRepo.save(Review.builder()
                .title("Sorogoyen en su mejor momento")
                .description("Esperaba mucho de esta película y ha cumplido con creces. " +
                        "La tensión entre padre e hija es incómoda en el mejor sentido de la palabra. " +
                        "Hay planos que son pura poesía visual. No es fácil de ver, pero merece " +
                        "mucho la pena. El cine español a su mejor nivel.")
                .rating(5).movie(m9).user(pabloG)
                .creationDate(hoy.minusDays(7).atTime(23, 30)).build());

        log.info("DataInitializer completado - {} películas, {} salas, {} sesiones, {} tickets.",
                movieRepo.count(), roomRepo.count(), sessionRepo.count(), ticketRepo.count());
    }
    private void seedPastPurchases(Session pasada1, Session pasada2, Session pasada3,
                                   Session sesAgotada, User user) {
        // MARCAR TICKETS COMPRADOS (historial de user1)
        // pasada1 → user1 compró 1 entradas (La odisea, hace 7 días)
        List<Ticket> tPasada1 = ticketRepo.findBySession_Id(pasada1.getId());
        if (tPasada1.size() >= 2) {
            tPasada1.get(0).setStatus(BuyStatus.PAGADO);
            tPasada1.get(0).setUser(user);
            tPasada1.get(0).setBuyDateTime(pasada1.getStartTime().minusHours(2));
            ticketRepo.save(tPasada1.get(0));
        }

        // pasada2 → user1 compró 1 entrada (Proyecto salvación, hace 5 días)
        List<Ticket> tPasada2 = ticketRepo.findBySession_Id(pasada2.getId());
        if (!tPasada2.isEmpty()) {
            tPasada2.get(0).setStatus(BuyStatus.PAGADO);
            tPasada2.get(0).setUser(user);
            tPasada2.get(0).setBuyDateTime(pasada2.getStartTime().minusDays(1));
            ticketRepo.save(tPasada2.get(0));
        }

        // pasada3 → user1 compró 3 entradas (The Mandalorian, hace 3 días)
        List<Ticket> tPasada3 = ticketRepo.findBySession_Id(pasada3.getId());
        if (!tPasada3.isEmpty()) {
            tPasada3.get(0).setStatus(BuyStatus.PAGADO);
            tPasada3.get(0).setUser(user);
            tPasada3.get(0).setBuyDateTime(pasada2.getStartTime().minusDays(1));
            ticketRepo.save(tPasada3.get(0));
        }

        // sesAgotada → TODOS los tickets PAGADO (demuestra el futuro badge "Agotada")
        List<Ticket> tAgotados = ticketRepo.findBySession_Id(sesAgotada.getId());
        tAgotados.forEach(t -> t.setStatus(BuyStatus.PAGADO));
        ticketRepo.saveAll(tAgotados);


    }

    private void seedTodayPurchases(Session[] sessions, User[] users) {
        Session todayOdisea4dx = sessions[0];
        Session todayMandalorian4dx = sessions[1];
        Session todayProyecto4dx = sessions[2];
        Session todayMarioImax = sessions[3];
        Session todayOdiseaImax = sessions[4];
        Session todayTopgunImax = sessions[5];
        Session todayTopgunStd = sessions[6];
        Session todayPlayaStd = sessions[7];
        User user = users[0];
        User user2 = users[1];
        User carlosR = users[2];
        User lauraS = users[3];
        User pabloG = users[4];
        User elenaM = users[5];
        User javierT = users[6];
        User sofiaL = users[7];

        // COMPRADORES FRECUENTES
        // Sala 1 · La odisea 4DX · hoy 16:30
        List<Ticket> ts0 = ticketRepo.findBySession_Id(todayOdisea4dx.getId());
        if (ts0.size() > 65) {
            ts0.get(1).setStatus(BuyStatus.PAGADO);  ts0.get(1).setUser(carlosR);
            ts0.get(1).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(4));
            ts0.get(12).setStatus(BuyStatus.PAGADO); ts0.get(12).setUser(lauraS);
            ts0.get(12).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(6));
            ts0.get(34).setStatus(BuyStatus.PAGADO); ts0.get(34).setUser(pabloG);
            ts0.get(34).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1));
            ts0.get(65).setStatus(BuyStatus.PAGADO); ts0.get(65).setUser(sofiaL);
            ts0.get(65).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1).minusHours(2));
            ticketRepo.saveAll(List.of(ts0.get(1), ts0.get(12), ts0.get(34), ts0.get(65)));
        }

        // Sala 1 · Mandalorian 4DX · hoy 19:00
        List<Ticket> ts1 = ticketRepo.findBySession_Id(todayMandalorian4dx.getId());
        if (ts1.size() > 55) {
            ts1.get(3).setStatus(BuyStatus.PAGADO);  ts1.get(3).setUser(elenaM);
            ts1.get(3).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(3));
            ts1.get(22).setStatus(BuyStatus.PAGADO); ts1.get(22).setUser(javierT);
            ts1.get(22).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(5));
            ts1.get(55).setStatus(BuyStatus.PAGADO); ts1.get(55).setUser(user2);
            ts1.get(55).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1));
            ticketRepo.saveAll(List.of(ts1.get(3), ts1.get(22), ts1.get(55)));
        }

        // Sala 1 · Proyecto salvación 4DX · hoy 21:30
        List<Ticket> ts2 = ticketRepo.findBySession_Id(todayProyecto4dx.getId());
        if (ts2.size() > 47) {
            ts2.get(9).setStatus(BuyStatus.PAGADO);  ts2.get(9).setUser(sofiaL);
            ts2.get(9).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(2));
            ts2.get(47).setStatus(BuyStatus.PAGADO); ts2.get(47).setUser(carlosR);
            ts2.get(47).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(7));
            ticketRepo.saveAll(List.of(ts2.get(9), ts2.get(47)));
        }

        // Sala 2 · Super Mario IMAX · hoy 16:00
        List<Ticket> ts3 = ticketRepo.findBySession_Id(todayMarioImax.getId());
        if (ts3.size() > 120) {
            ts3.get(2).setStatus(BuyStatus.PAGADO);   ts3.get(2).setUser(pabloG);
            ts3.get(2).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(2));
            ts3.get(25).setStatus(BuyStatus.PAGADO);  ts3.get(25).setUser(lauraS);
            ts3.get(25).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1));
            ts3.get(63).setStatus(BuyStatus.PAGADO);  ts3.get(63).setUser(elenaM);
            ts3.get(63).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1).minusHours(3));
            ts3.get(98).setStatus(BuyStatus.PAGADO);  ts3.get(98).setUser(javierT);
            ts3.get(98).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(2));
            ts3.get(120).setStatus(BuyStatus.PAGADO); ts3.get(120).setUser(user2);
            ts3.get(120).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(2).minusHours(4));
            ticketRepo.saveAll(List.of(ts3.get(2), ts3.get(25), ts3.get(63), ts3.get(98), ts3.get(120)));
        }

        // Sala 2 · La odisea IMAX · hoy 18:30
        List<Ticket> ts4 = ticketRepo.findBySession_Id(todayOdiseaImax.getId());
        if (ts4.size() > 88) {
            ts4.get(14).setStatus(BuyStatus.PAGADO); ts4.get(14).setUser(user2);
            ts4.get(14).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(1));
            ts4.get(37).setStatus(BuyStatus.PAGADO); ts4.get(37).setUser(carlosR);
            ts4.get(37).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(8));
            ts4.get(88).setStatus(BuyStatus.PAGADO); ts4.get(88).setUser(sofiaL);
            ts4.get(88).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1).minusHours(1));
            ticketRepo.saveAll(List.of(ts4.get(14), ts4.get(37), ts4.get(88)));
        }

        // Sala 2 · Top Gun IMAX · hoy 21:00
        List<Ticket> ts5 = ticketRepo.findBySession_Id(todayTopgunImax.getId());
        if (ts5.size() > 115) {
            ts5.get(4).setStatus(BuyStatus.PAGADO);   ts5.get(4).setUser(javierT);
            ts5.get(4).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(3));
            ts5.get(52).setStatus(BuyStatus.PAGADO);  ts5.get(52).setUser(pabloG);
            ts5.get(52).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(5));
            ts5.get(100).setStatus(BuyStatus.PAGADO); ts5.get(100).setUser(lauraS);
            ts5.get(100).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(1));
            ts5.get(115).setStatus(BuyStatus.PAGADO); ts5.get(115).setUser(elenaM);
            ts5.get(115).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusDays(2));
            ticketRepo.saveAll(List.of(ts5.get(4), ts5.get(52), ts5.get(100), ts5.get(115)));
        }

        // Sala 6 · Top Gun STANDARD · hoy 19:00
        List<Ticket> ts6 = ticketRepo.findBySession_Id(todayTopgunStd.getId());
        if (ts6.size() > 30) {
            ts6.get(5).setStatus(BuyStatus.PAGADO);  ts6.get(5).setUser(user);
            ts6.get(5).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(2));
            ts6.get(30).setStatus(BuyStatus.PAGADO); ts6.get(30).setUser(user2);
            ts6.get(30).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(4));
            ticketRepo.saveAll(List.of(ts6.get(5), ts6.get(30)));
        }

        // Sala 6 · Playa de lobos STANDARD · hoy 21:30
        List<Ticket> ts7 = ticketRepo.findBySession_Id(todayPlayaStd.getId());
        if (ts7.size() > 20) {
            ts7.get(8).setStatus(BuyStatus.PAGADO); ts7.get(8).setUser(carlosR);
            ts7.get(8).setBuyDateTime(LocalDateTime.now(CINEMA_ZONE).minusHours(1));
            ticketRepo.save(ts7.get(8));
        }


    }

}