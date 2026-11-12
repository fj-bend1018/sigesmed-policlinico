package com.sigesmed.api.dto.cita;

import jakarta.validation.constraints.Size;

public record CancelarCitaRequest(@Size(max = 500) String motivo) { }
