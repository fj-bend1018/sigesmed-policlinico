package com.sigesmed.api.dto.disponibilidad;

import java.util.List;

public record DoctorResponse(Long id, String nombre, String cmp, List<String> especialidades) { }
