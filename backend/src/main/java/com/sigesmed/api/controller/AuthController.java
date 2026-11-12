package com.sigesmed.api.controller;


import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.ApiMessage;
import com.sigesmed.api.dto.auth.AuthResponse;
import com.sigesmed.api.dto.auth.LoginRequest;
import com.sigesmed.api.dto.auth.RecuperacionRequest;
import com.sigesmed.api.dto.auth.RegistroPacienteRequest;
import com.sigesmed.api.dto.auth.RestablecerRequest;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(AuthService authService, SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/registro")
    public ResponseEntity<ApiMessage> registrar(@Valid @RequestBody RegistroPacienteRequest request) {
        Paciente paciente = authService.registrarPaciente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiMessage("Registro completado para el DNI " + paciente.getDni()));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Usuario usuario = authService.autenticar(request.identificador(), request.clave());
        HttpSession session = httpRequest.getSession(false);
        if (session == null) httpRequest.getSession(true);
        else httpRequest.changeSessionId();

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        return new AuthResponse(usuario.getId(), usuario.getDni(), usuario.getRol(), portal(usuario.getRol()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/recuperar")
    public ApiMessage recuperar(@Valid @RequestBody RecuperacionRequest request) {
        authService.solicitarRecuperacion(request.correo());
        return new ApiMessage("Si la cuenta existe, se enviarán instrucciones de recuperación");
    }

    @PostMapping("/auth/restablecer")
    public ApiMessage restablecer(@Valid @RequestBody RestablecerRequest request) {
        authService.restablecerClave(request.token(), request.nuevaClave());
        return new ApiMessage("Contraseña restablecida");
    }

    private String portal(RolUsuario rol) {
        return switch (rol) {
            case PACIENTE -> "paciente";
            case RECEPCIONISTA -> "recepcionista";
            case ADMINISTRADOR -> "administrador";
        };
    }
}
