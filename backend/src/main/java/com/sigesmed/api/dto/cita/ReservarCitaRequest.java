package com.sigesmed.api.dto.cita;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.AssertTrue;

public record ReservarCitaRequest(@NotNull @Positive Long horarioId,
        @Positive Long especialidadId, @Positive Long servicioId) {
    @AssertTrue(message = "Selecciona una especialidad o un servicio complementario")
    public boolean isAtencionValida() {
        return (especialidadId == null) != (servicioId == null);
    }
}
