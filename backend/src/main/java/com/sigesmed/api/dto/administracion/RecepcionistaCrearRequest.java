package com.sigesmed.api.dto.administracion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecepcionistaCrearRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos numéricos") String dni,
        @NotBlank @Email @Size(max = 254) String correo,
        @Pattern(regexp = "^$|^[0-9]{9}$", message = "El celular debe contener 9 dígitos") @Size(max = 9) String celular,
        @Size(max = 300) String direccion,
        @NotBlank @Size(min = 12, max = 72) String claveTemporal) { }
