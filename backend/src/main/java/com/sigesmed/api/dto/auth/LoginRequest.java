package com.sigesmed.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) @Pattern(regexp = "^(?:[0-9]{8}|[^\\s@]+@[^\\s@]+\\.[^\\s@]+)$",
                message = "Ingresa un DNI de 8 dígitos o un correo válido") String identificador,
        @NotBlank @Size(max = 72) String clave) { }
