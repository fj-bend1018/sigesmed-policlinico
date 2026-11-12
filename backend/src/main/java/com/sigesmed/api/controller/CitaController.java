package com.sigesmed.api.controller;

import java.net.URI;

import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.dto.cita.CancelarCitaRequest;
import com.sigesmed.api.dto.cita.ReprogramarCitaRequest;
import com.sigesmed.api.dto.cita.ReservarCitaRequest;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaService;
import com.sigesmed.api.service.CitaManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {
    private final CitaService citaService;
    private final CitaManagementService managementService;

    public CitaController(CitaService citaService, CitaManagementService managementService) {
        this.citaService = citaService;
        this.managementService = managementService;
    }

    @PostMapping
    public ResponseEntity<CitaResponse> reservar(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @Valid @RequestBody ReservarCitaRequest request) {
        CitaResponse cita = citaService.reservar(usuario.getId(), request.horarioId(),
                request.especialidadId(), request.servicioId());
        return ResponseEntity.created(URI.create("/api/v1/citas/" + cita.id())).body(cita);
    }

    @GetMapping("/{id}")
    public CitaResponse obtener(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id) {
        return citaService.obtenerPropia(usuario.getId(), id);
    }

    @GetMapping("/mias")
    public List<CitaResponse> mias(@AuthenticationPrincipal UsuarioPrincipal usuario,
            @RequestParam(defaultValue = "PROXIMAS") String vista) {
        return managementService.listarPropias(usuario.getId(), vista);
    }

    @PostMapping("/{id}/cancelacion")
    public CitaResponse cancelar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelarCitaRequest request) {
        return managementService.cancelarPropia(usuario.getId(), id, request == null ? null : request.motivo());
    }

    @PutMapping("/{id}/horario")
    public CitaResponse reprogramar(@AuthenticationPrincipal UsuarioPrincipal usuario, @PathVariable Long id,
            @Valid @RequestBody ReprogramarCitaRequest request) {
        return managementService.reprogramarPropia(usuario.getId(), id, request.horarioId());
    }
}
