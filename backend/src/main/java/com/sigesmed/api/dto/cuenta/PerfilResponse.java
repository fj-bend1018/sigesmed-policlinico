package com.sigesmed.api.dto.cuenta;

import com.sigesmed.api.domain.enums.RolUsuario;

public record PerfilResponse(RolUsuario rol, String dni, String nombre, String apellidoPaterno,
        String apellidoMaterno, String correo, String celular, String direccion) { }
