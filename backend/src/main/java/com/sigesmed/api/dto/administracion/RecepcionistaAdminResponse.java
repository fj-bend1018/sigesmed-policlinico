package com.sigesmed.api.dto.administracion;

public record RecepcionistaAdminResponse(Long id, String nombre, String apellidoPaterno, String apellidoMaterno,
        String dni, String correo, String celular, String direccion, String estado) { }
