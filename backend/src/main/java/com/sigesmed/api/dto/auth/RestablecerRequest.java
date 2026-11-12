package com.sigesmed.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerRequest(@NotBlank String token,
        @NotBlank @Size(max = 72, message = "La contraseña excede el máximo admitido por BCrypt") String nuevaClave) { }
