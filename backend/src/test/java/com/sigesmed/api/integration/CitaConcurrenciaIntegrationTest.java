package com.sigesmed.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import com.sigesmed.api.service.CitaManagementService;
import com.sigesmed.api.service.CitaService;

@Tag("integration")
@SpringBootTest
class CitaConcurrenciaIntegrationTest {

    @Autowired private CitaService citaService;
    @Autowired private CitaManagementService citaManagementService;
    @Autowired private HorarioRepository horarioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private Clock clock;

    private Doctor doctorTest;
    private Especialidad especialidadTest;
    private Usuario usuarioA;
    private Usuario usuarioB;
    private Paciente pacienteA;
    private Paciente pacienteB;
    private final List<Long> createdCitaIds = new ArrayList<>();
    private final List<Long> createdHorarioIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        assertEquals(ZoneId.of("America/Lima"), clock.getZone(), "El reloj debe estar en America/Lima");

        especialidadTest = especialidadRepository.findAll().stream()
                .filter(e -> !doctorRepository.findActiveBySpecialty(e.getId()).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay especialidades con doctores activos"));
        doctorTest = doctorRepository.findActiveBySpecialty(especialidadTest.getId()).getFirst();

        usuarioA = obtenerOCrearPaciente("88880001", "pacienteA@sigesmed.test", "Paciente A");
        pacienteA = pacienteRepository.findByUsuario_Id(usuarioA.getId()).orElseThrow();

        usuarioB = obtenerOCrearPaciente("88880002", "pacienteB@sigesmed.test", "Paciente B");
        pacienteB = pacienteRepository.findByUsuario_Id(usuarioB.getId()).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        for (Long citaId : createdCitaIds) {
            try {
                citaRepository.deleteById(citaId);
            } catch (Exception ignored) { }
        }
        for (Long horarioId : createdHorarioIds) {
            try {
                horarioRepository.deleteById(horarioId);
            } catch (Exception ignored) { }
        }
        if (usuarioA != null) {
            try {
                pacienteRepository.findByUsuario_Id(usuarioA.getId()).ifPresent(pacienteRepository::delete);
                usuarioRepository.deleteById(usuarioA.getId());
            } catch (Exception ignored) { }
        }
        if (usuarioB != null) {
            try {
                pacienteRepository.findByUsuario_Id(usuarioB.getId()).ifPresent(pacienteRepository::delete);
                usuarioRepository.deleteById(usuarioB.getId());
            } catch (Exception ignored) { }
        }
    }

    private Usuario obtenerOCrearPaciente(String dni, String correo, String nombre) {
        return usuarioRepository.findByDni(dni).orElseGet(() -> {
            Usuario u = usuarioRepository.save(new Usuario(dni, "$2a$10$dummyHashNotUsedDirectly", RolUsuario.PACIENTE));
            pacienteRepository.save(new Paciente(u, nombre, "Test", "Integracion", dni,
                    LocalDate.of(2000, 1, 1), correo));
            return u;
        });
    }

    @Test
    void testReservaConcurrenteMismoHorario_SoloUnaDebeTenerExito() throws InterruptedException {
        LocalDate fecha = LocalDate.now(clock).plusDays(15);
        LocalTime hora = LocalTime.of(16, 30);

        Horario horario = new Horario(doctorTest, fecha, hora);
        horario = horarioRepository.saveAndFlush(horario);
        createdHorarioIds.add(horario.getId());

        final Long horarioId = horario.getId();
        final Long espId = especialidadTest.getId();

        int nThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(nThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(nThreads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger conflicts = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        executor.submit(() -> {
            try {
                startGate.await();
                CitaResponse res = citaService.reservar(usuarioA.getId(), horarioId, espId);
                createdCitaIds.add(res.id());
                successes.incrementAndGet();
            } catch (ApiException e) {
                if (e.getStatus() == HttpStatus.CONFLICT) {
                    conflicts.incrementAndGet();
                } else {
                    errors.add(e);
                }
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startGate.await();
                CitaResponse res = citaService.reservar(usuarioB.getId(), horarioId, espId);
                createdCitaIds.add(res.id());
                successes.incrementAndGet();
            } catch (ApiException e) {
                if (e.getStatus() == HttpStatus.CONFLICT) {
                    conflicts.incrementAndGet();
                } else {
                    errors.add(e);
                }
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Las peticiones concurrentes deben completarse en menos de 10s");
        assertTrue(errors.isEmpty(), "No deben haber errores inesperados: " + errors);
        assertEquals(1, successes.get(), "Exactamente UNA reserva concurrente debe tener éxito");
        assertEquals(1, conflicts.get(), "Exactamente UNA reserva concurrente debe fallar con conflicto (409)");

        Horario horarioActualizado = horarioRepository.findById(horarioId).orElseThrow();
        assertEquals(EstadoHorario.OCUPADO, horarioActualizado.getEstado());
        assertTrue(!horarioActualizado.isDisponible());
    }

    @Test
    void testCancelarYReprogramarConRelojLima() {
        // 1. Cita con menos de 2 horas de anticipación: debe fallar con 409
        java.time.LocalDateTime dtCerca = java.time.LocalDateTime.now(clock).plusMinutes(60);
        LocalDate fechaCerca = dtCerca.toLocalDate();
        LocalTime horaCercana = dtCerca.toLocalTime().withSecond(0).withNano(0);

        Horario horarioCerca = new Horario(doctorTest, fechaCerca, horaCercana);
        horarioCerca.ocupar();
        horarioCerca = horarioRepository.saveAndFlush(horarioCerca);
        createdHorarioIds.add(horarioCerca.getId());

        Cita citaCerca = citaRepository.saveAndFlush(
                new Cita(pacienteA, horarioCerca, especialidadTest, EstadoCita.RESERVADA, OffsetDateTime.now(clock)));
        createdCitaIds.add(citaCerca.getId());

        ApiException exCancel = assertThrows(ApiException.class, () ->
                citaManagementService.cancelarPropia(usuarioA.getId(), citaCerca.getId(), "Cancelación tardía"));
        assertEquals(HttpStatus.CONFLICT, exCancel.getStatus());
        assertTrue(exCancel.getMessage().contains("al menos 2 horas"), "Mensaje debe indicar plazo de 2 horas: " + exCancel.getMessage());

        // 2. Cita con más de 2 horas de anticipación: debe cancelar y liberar horario
        LocalDate fechaFutura = LocalDate.now(clock).plusDays(20);
        LocalTime horaFutura = LocalTime.of(14, 15);

        Horario horarioLejos = new Horario(doctorTest, fechaFutura, horaFutura);
        horarioLejos.ocupar();
        horarioLejos = horarioRepository.saveAndFlush(horarioLejos);
        createdHorarioIds.add(horarioLejos.getId());

        Cita citaLejos = citaRepository.saveAndFlush(
                new Cita(pacienteA, horarioLejos, especialidadTest, EstadoCita.RESERVADA, OffsetDateTime.now(clock)));
        createdCitaIds.add(citaLejos.getId());

        CitaResponse cancelada = citaManagementService.cancelarPropia(usuarioA.getId(), citaLejos.getId(), "Cancelado con tiempo");
        assertNotNull(cancelada);
        assertEquals(EstadoCita.CANCELADA, cancelada.estado());

        Horario horarioLiberado = horarioRepository.findById(horarioLejos.getId()).orElseThrow();
        assertEquals(EstadoHorario.DISPONIBLE, horarioLiberado.getEstado());
        assertTrue(horarioLiberado.isDisponible());

        // 3. Reprogramar con más de 2 horas de anticipación: transfiere horario correctamente
        Horario horarioOrigen = new Horario(doctorTest, fechaFutura, LocalTime.of(15, 15));
        horarioOrigen.ocupar();
        horarioOrigen = horarioRepository.saveAndFlush(horarioOrigen);
        createdHorarioIds.add(horarioOrigen.getId());

        Horario horarioDestino = new Horario(doctorTest, fechaFutura, LocalTime.of(16, 15));
        horarioDestino = horarioRepository.saveAndFlush(horarioDestino);
        createdHorarioIds.add(horarioDestino.getId());

        Cita citaParaReprogramar = citaRepository.saveAndFlush(
                new Cita(pacienteA, horarioOrigen, especialidadTest, EstadoCita.RESERVADA, OffsetDateTime.now(clock)));
        createdCitaIds.add(citaParaReprogramar.getId());

        CitaResponse reprogramada = citaManagementService.reprogramarPropia(usuarioA.getId(), citaParaReprogramar.getId(), horarioDestino.getId());
        assertEquals(horarioDestino.getId(), reprogramada.horarioId());

        Horario origenLiberado = horarioRepository.findById(horarioOrigen.getId()).orElseThrow();
        assertEquals(EstadoHorario.DISPONIBLE, origenLiberado.getEstado());

        Horario destinoOcupado = horarioRepository.findById(horarioDestino.getId()).orElseThrow();
        assertEquals(EstadoHorario.OCUPADO, destinoOcupado.getEstado());
    }
}
