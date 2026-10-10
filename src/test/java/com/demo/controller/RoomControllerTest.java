package com.demo.controller;

import com.demo.model.Room;
import com.demo.model.enums.ScreenType;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// imports importantes


@SpringBootTest // Activa Spring
@AutoConfigureMockMvc(addFilters = false) // Activa MockMvc para testing de controller
@Transactional // deshace los cambios al final de cada test para no afectar al siguiente test
class RoomControllerTest {
    // importar mockMVC
    @Autowired
    MockMvc mockMvc;

    // importar repos
    @Autowired
    RoomRepository roomRepository;

    @Autowired
    SessionRepository sessionRepository;

    Room room;

    @BeforeEach
    void setUp() {
        sessionRepository.deleteAll();
        roomRepository.deleteAll();
        room = roomRepository.save(
                Room.builder()
                        .active(true)
                        .name("Sala 1")
                        .screenType(ScreenType.IMAX)
                        .capacity(100)
                        .build()
        );
    }

    // Comprobar que la lista se carga con status 200,
    // la vista correcta (rooms/room-list)
    // y que el modelo tiene el atributo "rooms" con exactamente 1 sala (la del setUp).
    @Test
    void listTableHeadersUseCssWidths() throws Exception {
        mockMvc.perform(get("/salas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("style=\"width: 25%;\"")))
                .andExpect(content().string(not(containsString("scope=\"col\" width="))));
    }

    @Test
    void listarSalas() throws Exception {
        mockMvc.perform(get("/salas"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/room-list"))
                .andExpect(model().attributeExists("rooms"))
                .andExpect(model().attribute("rooms", hasSize(1)));
    }

    // Usa el room.getId() del setUp para hacer GET
    // y verifica que llega a rooms/room-detail
    @Test
    void verDetalleSala_existe() throws Exception {
        mockMvc.perform(get("/salas/" + room.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/room-detail"))
                .andExpect(model().attributeExists("room"));
    }

    // ahora que no exista el id
    @Test
    void verDetalleSala_noExiste() throws Exception {
        // El controller ahora hace redirect:/salas cuando la sala no existe,
        // Así Thymeleaf carga "rooms" en el modelo
        // correctamente a través del endpoint /salas, evitando el null en la plantilla.
        Long idChungo = 99999L;

        mockMvc.perform(get("/salas/" + idChungo))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"));
    }

    //3 pasos: verificar estado inicial (que active = true),
    // desactivar, y que te redirija. Y verificar en repositorio que active = false.

    @Test
    void desactivarSala() throws Exception {
        assertTrue(room.getActive());

        Long id = room.getId();

        mockMvc.perform(post("/salas/deactivate/" + id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"));

        // Buscamos en BD para confirmar que active cambió a false
        Room room = roomRepository.findById(id).orElseThrow();
        assertFalse(room.getActive());
    }

    // Solo comprobar que la vista es rooms/room-form
    // y que el modelo tiene el atributo "room" (que el controller crea con new Room()).
    @Test
    void verFormularioCreacion() throws Exception {
        mockMvc.perform(get("/salas/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/room-form"))
                .andExpect(model().attributeExists("room"));
    }

    @Test
    void crearSala() throws Exception {
        // Guardamos el número de salas antes para comparar después
        long countBefore = roomRepository.count();

        mockMvc.perform(post("/salas")
                        .param("name", "sala test")
                        .param("capacity", "50")
                        .param("screenType", ScreenType.IMAX.toString())
                ).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"));

        // Verificamos que se creó exactamente 1 sala nueva, no más
        assertEquals(countBefore + 1, roomRepository.count());

        // Verificamos que los datos persistidos son los que enviamos
        Room roomGuardada = roomRepository.findAll().getLast();
        assertEquals("sala test", roomGuardada.getName());
    }

    //endpoint get del edit
    // Comprueba que el formulario de edición carga la sala existente en el modelo
    @Test
    void verEdicionSala() throws Exception {
        mockMvc.perform(get("/salas/edit/" + room.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("rooms/room-form"))
                .andExpect(model().attribute("room", hasProperty("id", is(room.getId()))));
    }

    //endpoint post
    @Test
    void editarSala() throws Exception {
        //Guardar valores originales para comparar
        Long id = room.getId();
        Optional<Room> roomBefore = roomRepository.findById(id);
        assertTrue(roomBefore.isPresent());

        long countBefore = roomRepository.count();

        //hacer el  post al endpoint de edición
        mockMvc.perform(post("/salas")
                        .param("id", roomBefore.get().getId().toString())
                        .param("name", "sala testerrr")
                        .param("capacity", "200")
                        .param("screenType", ScreenType.IMAX.toString())
                ).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"));

        // Editar no debe crear una sala nueva, el count tiene que ser el mismo
        assertEquals(countBefore, roomRepository.count());

        //buscar la sala editada en el repositorio
        Room roomAfter = roomRepository.findById(room.getId()).orElseThrow();

        assertEquals("sala testerrr", roomAfter.getName());
        assertEquals(200, roomAfter.getCapacity());
        assertEquals(ScreenType.IMAX, roomAfter.getScreenType());
        assertEquals(true, roomAfter.getActive());
    }
    @Test
    void editingRoomCannotAlterActiveThroughHiddenField() throws Exception {
        mockMvc.perform(post("/salas")
                        .param("id", room.getId().toString())
                        .param("name", "Sala segura")
                        .param("capacity", "90")
                        .param("screenType", "IMAX")
                        .param("active", "false"))
                .andExpect(status().is3xxRedirection());
        Room persisted = roomRepository.findById(room.getId()).orElseThrow();
        assertTrue(persisted.getActive());
        assertEquals("Sala segura", persisted.getName());
    }

}
