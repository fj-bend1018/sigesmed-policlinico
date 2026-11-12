package com.sigesmed.api.controller;

import com.sigesmed.api.dto.cuenta.ActualizarPerfilRequest;
import com.sigesmed.api.dto.cuenta.PerfilResponse;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuenta/me")
public class CuentaController {
    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) { this.cuentaService = cuentaService; }

    @GetMapping
    public PerfilResponse obtener(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return cuentaService.obtener(usuario.getId());
    }

    @PutMapping
    public PerfilResponse actualizar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return cuentaService.actualizar(usuario.getId(), request);
    }
}
