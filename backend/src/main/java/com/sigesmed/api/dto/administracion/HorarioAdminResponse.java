package com.sigesmed.api.dto.administracion;

import java.time.LocalDate;
import java.time.LocalTime;

import com.sigesmed.api.domain.enums.EstadoHorario;

public record HorarioAdminResponse(Long id, Long doctorId, String doctor, Long servicioId, String servicio,
        LocalDate fecha, LocalTime hora, boolean disponible, EstadoHorario estado) { }
