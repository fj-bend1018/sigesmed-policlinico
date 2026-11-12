package com.sigesmed.api.dto.administracion;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DoctorAdminRequest(
        @NotBlank @Size(max = 160) String nombre,
        @NotBlank @Size(max = 40) String cmp,
        @NotEmpty List<@NotNull @Positive Long> especialidadIds) { }
