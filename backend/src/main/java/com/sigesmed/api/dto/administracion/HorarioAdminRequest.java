package com.sigesmed.api.dto.administracion;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

public record HorarioAdminRequest(@Positive Long doctorId, @Positive Long servicioId,
        @NotNull LocalDate fecha, @NotNull LocalTime hora) {
    @AssertTrue(message = "Selecciona un médico o un servicio complementario, no ambos")
    public boolean isRecursoValido() {
        return (doctorId == null) != (servicioId == null);
    }
}
