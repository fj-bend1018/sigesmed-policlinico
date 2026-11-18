package com.sigesmed.api.dto.auth;

import com.sigesmed.api.domain.enums.RolUsuario;

public record AuthResponse(Long usuarioId, String dni, RolUsuario rol, String portal, String nombre) {
    public AuthResponse(Long usuarioId, String dni, RolUsuario rol, String portal) {
        this(usuarioId, dni, rol, portal, null);
    }
}
