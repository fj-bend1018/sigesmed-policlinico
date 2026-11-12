package com.sigesmed.api.dto.administracion;

import java.util.List;

public record DoctorAdminResponse(Long id, String nombre, String cmp, String estado, List<Long> especialidadIds,
        List<String> especialidades) { }
