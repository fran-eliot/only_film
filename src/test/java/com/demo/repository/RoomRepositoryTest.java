package com.demo.repository;

import com.demo.model.Room;
import com.demo.model.enums.ScreenType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//filtro el acceso a los datos
@DataJpaTest
class RoomRepositoryTest {

    @Autowired
    RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        roomRepository.saveAll(
                List.of(
                        Room.builder().name("Salita uno").screenType(ScreenType.D4X).capacity(100).build(),
                        Room.builder().name("Salita due").screenType(ScreenType.IMAX).capacity(150).build(),
                        Room.builder().name("Salita tresss").screenType(ScreenType.D4X).capacity(80).build(),
                        Room.builder().name("Salita cuatre").screenType(ScreenType.IMAX).capacity(120).build(),
                        Room.builder().name("Salita ultima").screenType(ScreenType.D4X).capacity(90).build()
                ));
    }

    //buscar cuantas salas existen
    //findByNumberRooms
    @Test
    void findByNumberRooms() {
        List<Room> rooms = roomRepository.findAll();
        assertEquals(5, rooms.size());

    }

    //buscar por tipo de pantalla
    //findByScreenType
    @Test
    void findByScreenType() {
        List<Room> rooms = roomRepository.findByScreenType(ScreenType.D4X);
        assertEquals(3, rooms.size());
    }


    //buscar por capacidad de la sala
    //findByCapacidadGreaterThan
    @Test
    void findByCapacidadGreaterThan() {
        List<Room> rooms = roomRepository.findByCapacityGreaterThan(100);

        assertEquals(2, rooms.size());
        assertEquals(java.util.Set.of("Salita due", "Salita cuatre"),
                rooms.stream().map(Room::getName).collect(java.util.stream.Collectors.toSet()));
        assertTrue(rooms.stream().allMatch(room -> room.getCapacity() > 100));

        // La capacidad exactamente igual al umbral no debe incluirse.
        List<Room> aboveNinetyNine = roomRepository.findByCapacityGreaterThan(99);
        assertEquals(3, aboveNinetyNine.size());
        assertTrue(aboveNinetyNine.stream().anyMatch(room -> room.getCapacity() == 100));

        assertTrue(roomRepository.findByCapacityGreaterThan(150).isEmpty());
    }


    @Test
    void builderDefaultsRoomToActiveButRespectsExplicitDeactivation() {
        Room defaultActive = roomRepository.save(Room.builder()
                .name("Sala nueva").capacity(60).build());
        Room disabled = roomRepository.save(Room.builder()
                .name("Sala cerrada").capacity(60).active(false).build());

        assertTrue(defaultActive.getActive());
        assertFalse(disabled.getActive());

        List<Room> activeRooms = roomRepository.findAllByActiveTrue();
        assertTrue(activeRooms.stream().anyMatch(room -> room.getId().equals(defaultActive.getId())));
        assertFalse(activeRooms.stream().anyMatch(room -> room.getId().equals(disabled.getId())));
    }

    @Test
    void findByIdAndActiveTrueExcludesInactiveRooms() {
        Room activeRoom = roomRepository.save(Room.builder()
                .name("Sala habilitada").capacity(60).active(true).build());
        Room inactiveRoom = roomRepository.save(Room.builder()
                .name("Sala deshabilitada").capacity(60).active(false).build());

        assertEquals(activeRoom.getId(), roomRepository
                .findByIdAndActiveTrue(activeRoom.getId()).orElseThrow().getId());
        assertEquals(1, roomRepository.findAllByIdAndActiveTrue(activeRoom.getId()).size());
        assertEquals(activeRoom.getId(),
                roomRepository.findAllByIdAndActiveTrue(activeRoom.getId()).getFirst().getId());

        assertTrue(roomRepository.findByIdAndActiveTrue(inactiveRoom.getId()).isEmpty());
        assertTrue(roomRepository.findAllByIdAndActiveTrue(inactiveRoom.getId()).isEmpty());
        assertTrue(roomRepository.findById(inactiveRoom.getId()).isPresent());

        assertTrue(roomRepository.findByIdAndActiveTrue(Long.MAX_VALUE).isEmpty());
        assertTrue(roomRepository.findAllByIdAndActiveTrue(Long.MAX_VALUE).isEmpty());
    }

    @AfterEach
    void tearDown() {
        roomRepository.deleteAll();
    }
}