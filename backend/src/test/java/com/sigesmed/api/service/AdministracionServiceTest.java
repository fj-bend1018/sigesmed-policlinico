package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdministracionServiceTest {
    @Mock private DoctorRepository doctores;
    @Mock private EspecialidadRepository especialidades;
    @Mock private HorarioRepository horarios;
    @Mock private CitaRepository citas;
    @Mock private RecepcionistaRepository recepcionistas;
    @Mock private UsuarioRepository usuarios;
    @Mock private PasswordEncoder encoder;

    private AdministracionService service;

    @BeforeEach
    void setUp() {
        service = new AdministracionService(doctores, especialidades, horarios, citas, recepcionistas,
                usuarios, encoder, Clock.fixed(java.time.Instant.parse("2026-11-08T15:00:00Z"),
                        ZoneId.of("America/Lima")));
    }

    @Test
    void eliminarHorarioRechazaCitaActivaCon409() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-DEMO-1", java.util.Set.of());
        Horario horario = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(horario));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.eliminarHorario(50L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("No se puede eliminar el horario porque tiene una cita reservada o confirmada",
                error.getMessage());
        verify(horarios, never()).delete(any(Horario.class));
    }
}
