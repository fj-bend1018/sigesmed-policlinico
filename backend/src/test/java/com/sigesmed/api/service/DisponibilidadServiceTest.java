package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.dto.disponibilidad.DoctorResponse;
import com.sigesmed.api.dto.disponibilidad.EspecialidadResponse;
import com.sigesmed.api.dto.disponibilidad.HorarioResponse;
import com.sigesmed.api.dto.disponibilidad.ServicioComplementarioResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    // 2026-11-08 15:00:00 UTC = 2026-11-08 10:00:00 Lima
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), LIMA);

    @Mock private EspecialidadRepository especialidades;
    @Mock private DoctorRepository doctores;
    @Mock private HorarioRepository horarios;
    @Mock private CitaRepository citas;
    @Mock private ServicioComplementarioRepository servicios;

    private DisponibilidadService service;

    @BeforeEach
    void setUp() {
        service = new DisponibilidadService(especialidades, doctores, horarios, citas, servicios, clock);
    }

    // === ESPECIALIDADES ===

    @Test
    void especialidades_retornaListaOrdenada() {
        Especialidad e1 = new Especialidad("Medicina familiar");
        ReflectionTestUtils.setField(e1, "id", 1L);
        Especialidad e2 = new Especialidad("Pediatría general");
        ReflectionTestUtils.setField(e2, "id", 2L);

        when(especialidades.findAllByOrderByNombreAsc()).thenReturn(List.of(e1, e2));

        List<EspecialidadResponse> list = service.especialidades();
        assertEquals(2, list.size());
        assertEquals("Medicina familiar", list.get(0).nombre());
        assertEquals("Pediatría general", list.get(1).nombre());
    }

    // === SERVICIOS COMPLEMENTARIOS ===

    @Test
    void serviciosComplementarios_retornaListaOrdenada() {
        ServicioComplementario s1 = new ServicioComplementario("Ecografía");
        ReflectionTestUtils.setField(s1, "id", 10L);
        ServicioComplementario s2 = new ServicioComplementario("Laboratorio");
        ReflectionTestUtils.setField(s2, "id", 20L);

        when(servicios.findAllByOrderByNombreAsc()).thenReturn(List.of(s1, s2));

        List<ServicioComplementarioResponse> list = service.serviciosComplementarios();
        assertEquals(2, list.size());
        assertEquals("Ecografía", list.get(0).nombre());
        assertEquals("Laboratorio", list.get(1).nombre());
    }

    // === MÉDICOS POR ESPECIALIDAD ===

    @Test
    void medicos_especialidadNoExisteLanza404() {
        when(especialidades.existsById(99L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> service.medicos(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Especialidad no encontrada", ex.getMessage());
    }

    @Test
    void medicos_especialidadExisteRetornaMedicosActivos() {
        Especialidad esp = new Especialidad("Pediatría general");
        ReflectionTestUtils.setField(esp, "id", 1L);

        Doctor doc = new Doctor("Dra. Ana Demo", "CMP-001", Set.of(esp));
        ReflectionTestUtils.setField(doc, "id", 10L);

        when(especialidades.existsById(1L)).thenReturn(true);
        when(doctores.findActiveBySpecialty(1L)).thenReturn(List.of(doc));

        List<DoctorResponse> medicos = service.medicos(1L);
        assertEquals(1, medicos.size());
        assertEquals("Dra. Ana Demo", medicos.getFirst().nombre());
        assertEquals("CMP-001", medicos.getFirst().cmp());
        assertEquals(List.of("Pediatría general"), medicos.getFirst().especialidades());
    }

    // === HORARIOS POR MÉDICO ===

    @Test
    void horarios_fechaPasadaLanza400() {
        LocalDate ayer = LocalDate.of(2026, 11, 7);
        ApiException ex = assertThrows(ApiException.class, () -> service.horarios(1L, ayer, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("La fecha debe ser hoy o futura", ex.getMessage());
    }

    @Test
    void horarios_medicoNoExisteOInactivoLanza404() {
        LocalDate hoy = LocalDate.of(2026, 11, 8);
        when(doctores.findByIdAndEstado(99L, "ACTIVO")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.horarios(99L, hoy, null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Médico no encontrado o inactivo", ex.getMessage());
    }

    @Test
    void horariosOcultaHorasQueYaTieneReservadasElPacienteEnOtraEspecialidad() {
        LocalDate date = LocalDate.of(2026, 11, 9);
        Doctor doctor = new Doctor("Dra. Demo", "CMP-TEST-1", Set.of());
        ReflectionTestUtils.setField(doctor, "id", 12L);
        Horario nine = new Horario(doctor, date, LocalTime.of(9, 0));
        Horario ten = new Horario(doctor, date, LocalTime.of(10, 0));
        ReflectionTestUtils.setField(nine, "id", 21L);
        ReflectionTestUtils.setField(ten, "id", 22L);

        when(doctores.findByIdAndEstado(12L, "ACTIVO")).thenReturn(Optional.of(doctor));
        when(citas.findActiveTimesForUserAndDate(90L, date, EstadoCita.ACTIVAS)).thenReturn(List.of(LocalTime.of(10, 0)));
        when(horarios.findAvailableByDoctorAndDate(12L, date, EstadoHorario.DISPONIBLE, EstadoCita.ACTIVAS))
                .thenReturn(List.of(nine, ten));

        var result = service.horarios(12L, date, 90L);

        assertEquals(1, result.size());
        assertEquals(LocalTime.of(9, 0), result.getFirst().hora());
    }

    @Test
    void horarios_diaDeHoyFiltraHorasPasadas() {
        LocalDate hoy = LocalDate.of(2026, 11, 8); // at 10:00 Lima
        Doctor doctor = new Doctor("Dr. Demo", "CMP-1", Set.of());
        ReflectionTestUtils.setField(doctor, "id", 5L);

        Horario nine = new Horario(doctor, hoy, LocalTime.of(9, 0)); // ya pasó
        Horario ten = new Horario(doctor, hoy, LocalTime.of(10, 0)); // no es posterior
        Horario eleven = new Horario(doctor, hoy, LocalTime.of(11, 0)); // futuro
        ReflectionTestUtils.setField(eleven, "id", 33L);

        when(doctores.findByIdAndEstado(5L, "ACTIVO")).thenReturn(Optional.of(doctor));
        when(horarios.findAvailableByDoctorAndDate(5L, hoy, EstadoHorario.DISPONIBLE, EstadoCita.ACTIVAS))
                .thenReturn(List.of(nine, ten, eleven));

        var result = service.horarios(5L, hoy, null);
        assertEquals(1, result.size());
        assertEquals(LocalTime.of(11, 0), result.getFirst().hora());
    }

    // === HORARIOS POR SERVICIO ===

    @Test
    void horariosServicio_fechaPasadaLanza400() {
        LocalDate ayer = LocalDate.of(2026, 11, 7);
        ApiException ex = assertThrows(ApiException.class, () -> service.horariosServicio(1L, ayer, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void horariosServicio_servicioNoExisteLanza404() {
        LocalDate manana = LocalDate.of(2026, 11, 9);
        when(servicios.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.horariosServicio(99L, manana, null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Servicio complementario no encontrado", ex.getMessage());
    }

    @Test
    void horariosServicio_exitosoFiltraHorasPasadasYCitasPaciente() {
        LocalDate manana = LocalDate.of(2026, 11, 9);
        ServicioComplementario serv = new ServicioComplementario("Ecografía");
        ReflectionTestUtils.setField(serv, "id", 8L);

        Horario h1 = new Horario(serv, manana, LocalTime.of(14, 0));
        Horario h2 = new Horario(serv, manana, LocalTime.of(15, 0));
        ReflectionTestUtils.setField(h1, "id", 81L);
        ReflectionTestUtils.setField(h2, "id", 82L);

        when(servicios.findById(8L)).thenReturn(Optional.of(serv));
        when(citas.findActiveTimesForUserAndDate(100L, manana, EstadoCita.ACTIVAS))
                .thenReturn(List.of(LocalTime.of(14, 0)));
        when(horarios.findAvailableByServiceAndDate(8L, manana, EstadoHorario.DISPONIBLE, EstadoCita.ACTIVAS))
                .thenReturn(List.of(h1, h2));

        var result = service.horariosServicio(8L, manana, 100L);
        assertEquals(1, result.size());
        assertEquals(82L, result.getFirst().id());
        assertEquals(LocalTime.of(15, 0), result.getFirst().hora());
        assertEquals("Ecografía", result.getFirst().servicioNombre());
    }
}
