package com.sigesmed.api.dto.cita;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegistrarPagoRequest(
        @NotBlank(message = "Selecciona un método de pago")
        @Pattern(regexp = "(?i)^(EFECTIVO|TARJETA|YAPE|PLIN)$", message = "Método de pago no válido")
        String metodo,
        @NotNull(message = "Ingresa el monto")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        @Digits(integer = 8, fraction = 2, message = "Formato de monto inválido")
        BigDecimal monto) { }
