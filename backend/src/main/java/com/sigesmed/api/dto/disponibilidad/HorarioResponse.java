package com.sigesmed.api.dto.disponibilidad;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioResponse(Long id, Long doctorId, String doctorNombre, Long servicioId,
        String servicioNombre, LocalDate fecha, LocalTime hora) { }
