package com.sigesmed.api.dto.administracion;

import java.util.List;

public record HorarioRecurrenteResponse(
        Long doctorId,
        String doctor,
        int creados,
        List<HorarioAdminResponse> horarios
) {
}
