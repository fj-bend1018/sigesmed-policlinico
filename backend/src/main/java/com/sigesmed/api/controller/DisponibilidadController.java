package com.sigesmed.api.controller;

import java.time.LocalDate;
import java.util.List;

import com.sigesmed.api.dto.disponibilidad.DoctorResponse;
import com.sigesmed.api.dto.disponibilidad.EspecialidadResponse;
import com.sigesmed.api.dto.disponibilidad.HorarioResponse;
import com.sigesmed.api.dto.disponibilidad.ServicioComplementarioResponse;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.DisponibilidadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DisponibilidadController {
    private final DisponibilidadService disponibilidadService;

    public DisponibilidadController(DisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/especialidades")
    public List<EspecialidadResponse> especialidades() {
        return disponibilidadService.especialidades();
    }

    @GetMapping("/servicios-complementarios")
    public List<ServicioComplementarioResponse> serviciosComplementarios() {
        return disponibilidadService.serviciosComplementarios();
    }

    @GetMapping("/servicios-complementarios/{id}/horarios")
    public List<HorarioResponse> horariosServicio(@PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Long pacienteUsuarioId = usuario != null && "PACIENTE".equals(usuario.getRole()) ? usuario.getId() : null;
        return disponibilidadService.horariosServicio(id, fecha, pacienteUsuarioId);
    }

    @GetMapping("/especialidades/{id}/medicos")
    public List<DoctorResponse> medicos(@PathVariable Long id) {
        return disponibilidadService.medicos(id);
    }

    @GetMapping("/medicos/{id}/horarios")
    public List<HorarioResponse> horarios(@PathVariable Long id, @AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Long pacienteUsuarioId = usuario != null && "PACIENTE".equals(usuario.getRole()) ? usuario.getId() : null;
        return disponibilidadService.horarios(id, fecha, pacienteUsuarioId);
    }
}
