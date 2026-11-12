package com.sigesmed.api.dto.administracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoRequest(@NotBlank @Pattern(regexp = "ACTIVO|INACTIVO") String estado) { }
