package com.sigesmed.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerRequest(@NotBlank String token,
        @NotBlank @Size(max = 18, message = "La contraseña no puede superar los 18 caracteres") String nuevaClave) { }
