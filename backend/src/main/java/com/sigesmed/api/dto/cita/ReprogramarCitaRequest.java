package com.sigesmed.api.dto.cita;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReprogramarCitaRequest(@NotNull @Positive Long horarioId) { }
