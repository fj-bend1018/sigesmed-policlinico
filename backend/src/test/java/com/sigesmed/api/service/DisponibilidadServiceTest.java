package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {
    @Mock private EspecialidadRepository especialidades;
    @Mock private DoctorRepository doctores;
    @Mock private HorarioRepository horarios;
    @Mock private CitaRepository citas;

    @Test
    void horariosOcultaHorasQueYaTieneReservadasElPacienteEnOtraEspecialidad() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), ZoneId.of("America/Lima"));
        DisponibilidadService service = new DisponibilidadService(especialidades, doctores, horarios, citas, clock);
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
}
