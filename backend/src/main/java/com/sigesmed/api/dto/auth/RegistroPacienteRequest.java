package com.sigesmed.api.dto.auth;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroPacienteRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellidoPaterno,
        @Size(max = 100) String apellidoMaterno,
        @NotBlank @Pattern(regexp = "^[0-9]{8}$", message = "El DNI debe tener 8 dígitos numéricos") String dni,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Size(max = 18, message = "La contraseña no puede superar los 18 caracteres") String clave,
        @Pattern(regexp = "^$|^[0-9]{9}$", message = "El celular debe tener 9 dígitos numéricos") String celular) {
    public RegistroPacienteRequest(String nombre, String apellidoPaterno, String apellidoMaterno,
            String dni, LocalDate fechaNacimiento, String correo, String clave) {
        this(nombre, apellidoPaterno, apellidoMaterno, dni, fechaNacimiento, correo, clave, null);
    }
}
