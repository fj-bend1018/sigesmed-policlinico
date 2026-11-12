package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CitaManagementServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Instant NOW = Instant.parse("2026-11-08T15:00:00Z");
    private final Clock clock = Clock.fixed(NOW, LIMA);

    @Mock private PacienteRepository pacientes;
    @Mock private CitaRepository citas;
    @Mock private HorarioRepository horarios;

    private CitaManagementService service;
    private Paciente patient;
    private Especialidad pediatria;
    private Especialidad odontologia;
    private Horario oldSlot;
    private Cita appointment;

    @BeforeEach
    void setUp() {
        service = new CitaManagementService(pacientes, citas, horarios, clock, Duration.ofHours(2));
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 10L);
        patient = new Paciente(user, "Ana", "Prueba", "Demo", "12345678",
                LocalDate.of(2000, 1, 1), "ana@example.test");
        ReflectionTestUtils.setField(patient, "id", 20L);
        pediatria = specialty(30L, "Pediatría");
        odontologia = specialty(31L, "Odontología");
        oldSlot = slot(50L, pediatria, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        appointment = new Cita(patient, oldSlot, pediatria, EstadoCita.RESERVADA,
                OffsetDateTime.now(clock));
        ReflectionTestUtils.setField(appointment, "id", 60L);
    }

    @Test
    void cancelarRechazaCitaConMenosDeDosHoras() {
        Horario soon = slot(51L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(11, 30));
        appointment.setHorario(soon);
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));

        ApiException error = assertThrows(ApiException.class, () -> service.cancelarPropia(10L, 60L, "motivo"));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).findByIdForUpdate(anyLong());
        verify(citas, never()).save(any(Cita.class));
    }

    @Test
    void cancelarAceptaElLimiteExactoDeDosHorasYLiberaElHorario() {
        Horario exactlyTwoHours = slot(51L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(12, 0));
        appointment.setHorario(exactlyTwoHours);
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(51L)).thenReturn(Optional.of(exactlyTwoHours));
        when(citas.save(appointment)).thenReturn(appointment);

        service.cancelarPropia(10L, 60L, "cambio de planes");

        assertEquals(EstadoCita.CANCELADA, appointment.getEstado());
        assertEquals(true, exactlyTwoHours.isDisponible());
        verify(horarios).save(exactlyTwoHours);
        verify(citas).save(appointment);
    }

    @Test
    void reprogramarRechazaOtraEspecialidad() {
        Horario newSlot = slot(52L, odontologia, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reprogramarRechazaHorarioOcupado() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        newSlot.ocupar();
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reprogramarRechazaHorarioQueNoEsFuturo() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(9, 59));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void pacienteNoPuedeObtenerCitaDeOtraPersona() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(citas, never()).findByIdForUpdate(anyLong());
        verify(horarios, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void reprogramacionValidaLiberaElHorarioAnteriorYOcupaElNuevo() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));
        when(citas.existsActiveForPatientAt(20L, newSlot.getFecha(), newSlot.getHora(),
                Set.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA))).thenReturn(false);
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(oldSlot));
        when(citas.saveAndFlush(appointment)).thenReturn(appointment);

        service.reprogramarPropia(10L, 60L, 52L);

        assertSame(newSlot, appointment.getHorario());
        assertEquals(true, oldSlot.isDisponible());
        assertEquals(false, newSlot.isDisponible());
        verify(horarios).save(oldSlot);
        verify(horarios).save(newSlot);
        verify(citas).saveAndFlush(appointment);
    }

    private Especialidad specialty(Long id, String name) {
        Especialidad value = new Especialidad(name);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Horario slot(Long id, Especialidad specialty, LocalDate date, LocalTime time) {
        Doctor doctor = new Doctor("Dr. Demo", "CMP-DEMO-" + id, Set.of(specialty));
        ReflectionTestUtils.setField(doctor, "id", id + 100);
        Horario value = new Horario(doctor, date, time);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
