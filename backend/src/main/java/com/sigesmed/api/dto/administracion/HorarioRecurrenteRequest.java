package com.sigesmed.api.dto.administracion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record HorarioRecurrenteRequest(
        @NotNull @Positive Long doctorId,
        @NotEmpty List<DayOfWeek> diasSemana,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @NotNull LocalDate fechaDesde,
        @NotNull LocalDate fechaHasta,
        List<LocalDate> diasDescanso
) {
}
