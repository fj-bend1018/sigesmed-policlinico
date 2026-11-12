package com.sigesmed.api.dto.administracion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecepcionistaActualizarRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Email @Size(max = 254) String correo,
        @Pattern(regexp = "^$|^[0-9]{9}$", message = "El celular debe contener 9 dígitos") @Size(max = 9) String celular,
        @Size(max = 300) String direccion) { }
