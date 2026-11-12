package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), ZoneId.of("America/Lima"));
    @Mock private PacienteRepository pacientes;
    @Mock private HorarioRepository horarios;
    @Mock private CitaRepository citas;
    @Mock private EspecialidadRepository especialidades;
    private CitaService service;
    private Paciente patient;
    private Horario slot;
    private Especialidad specialty;

    @BeforeEach
    void setUp() {
        service = new CitaService(pacientes, horarios, citas, especialidades, clock);
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 10L);
        patient = new Paciente(user, "Ana", "Prueba", "Demo", "12345678",
                LocalDate.of(2000, 1, 1), "ana@example.test");
        ReflectionTestUtils.setField(patient, "id", 20L);
        specialty = new Especialidad("Pediatría");
        ReflectionTestUtils.setField(specialty, "id", 30L);
        Doctor doctor = new Doctor("Dr. Demo", "CMP-DEMO-1", Set.of(specialty));
        ReflectionTestUtils.setField(doctor, "id", 40L);
        slot = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        ReflectionTestUtils.setField(slot, "id", 50L);
    }

    @Test
    void reservar_C1_pacienteNoExiste() {
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 30L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(horarios, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void reservar_C2_horarioNoExiste() {
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 30L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void reservar_C3_horarioNoDisponible() {
        slot.ocupar();
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(slot));
        ApiException exception = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 30L));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reservar_rechazaHorarioConCitaActivaAunqueFiguraDisponible() {
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(slot));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(true);

        ApiException exception = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 30L));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(especialidades, never()).findById(anyLong());
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reservar_C4_citaDuplicadaDelPacienteEnEseHorario() {
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(slot));
        when(especialidades.findById(30L)).thenReturn(Optional.of(specialty));
        when(citas.existsActiveForPatientAt(20L, slot.getFecha(), slot.getHora(), EstadoCita.ACTIVAS))
                .thenReturn(true);
        ApiException exception = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 30L));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reservar_C5_reservaExitosaEnEstadoRESERVADA() {
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(slot));
        when(especialidades.findById(30L)).thenReturn(Optional.of(specialty));
        when(citas.existsActiveForPatientAt(20L, slot.getFecha(), slot.getHora(), EstadoCita.ACTIVAS))
                .thenReturn(false);
        when(citas.saveAndFlush(any(Cita.class))).thenAnswer(invocation -> {
            Cita saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 60L);
            return saved;
        });

        var response = service.reservar(10L, 50L, 30L);
        ArgumentCaptor<Cita> citaCaptor = ArgumentCaptor.forClass(Cita.class);
        verify(citas).saveAndFlush(citaCaptor.capture());
        assertEquals(EstadoCita.RESERVADA, citaCaptor.getValue().getEstado());
        assertEquals(60L, response.id());
        assertEquals(false, slot.isDisponible());
        assertEquals(com.sigesmed.api.domain.enums.EstadoHorario.OCUPADO, slot.getEstado());
        verify(horarios).save(slot);
    }

    @Test
    void reservar_rechazaEspecialidadQueNoOfreceElMedico() {
        Especialidad otra = new Especialidad("Odontología");
        ReflectionTestUtils.setField(otra, "id", 31L);
        when(pacientes.findByUsuarioIdForUpdate(10L)).thenReturn(Optional.of(patient));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(slot));
        when(especialidades.findById(31L)).thenReturn(Optional.of(otra));

        ApiException error = assertThrows(ApiException.class, () -> service.reservar(10L, 50L, 31L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void obtenerPropia_ocultaCitaDeOtroPaciente() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_Id(60L, 20L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class, () -> service.obtenerPropia(10L, 60L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
