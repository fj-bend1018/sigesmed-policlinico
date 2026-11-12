package com.sigesmed.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacionRequest(@NotBlank @Email @Size(max = 254) String correo) { }
