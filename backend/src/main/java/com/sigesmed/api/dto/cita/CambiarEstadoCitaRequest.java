package com.sigesmed.api.dto.cita;

import com.sigesmed.api.domain.enums.EstadoCita;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoCitaRequest(@NotNull EstadoCita estado) { }
