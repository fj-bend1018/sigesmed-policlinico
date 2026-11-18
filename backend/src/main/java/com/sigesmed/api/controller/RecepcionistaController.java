package com.sigesmed.api.controller;

import java.time.LocalDate;
import java.util.List;

import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.dto.cita.CambiarEstadoCitaRequest;
import com.sigesmed.api.dto.cita.CancelarCitaRequest;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.dto.cita.ComprobantePagoResponse;
import com.sigesmed.api.dto.cita.RegistrarPagoRequest;
import com.sigesmed.api.dto.cita.ReprogramarCitaRequest;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/recepcionista/citas")
public class RecepcionistaController {
    private final CitaManagementService service;

    public RecepcionistaController(CitaManagementService service) { this.service = service; }

    @GetMapping
    public List<CitaResponse> listar(@RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String buscar) {
        return service.listarAgenda(estado, fecha, buscar);
    }

    @GetMapping("/{id}")
    public CitaResponse obtener(@PathVariable @Positive Long id) { return service.obtenerParaRecepcion(id); }

    @PostMapping("/{id}/cancelacion")
    public CitaResponse cancelar(@PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) CancelarCitaRequest request) {
        return service.cancelarPorRecepcion(id, request == null ? null : request.motivo());
    }

    @PutMapping("/{id}/horario")
    public CitaResponse reprogramar(@PathVariable @Positive Long id,
            @Valid @RequestBody ReprogramarCitaRequest request) {
        return service.reprogramarPorRecepcion(id, request.horarioId());
    }

    @PatchMapping("/{id}/estado")
    public CitaResponse estado(@PathVariable @Positive Long id,
            @Valid @RequestBody CambiarEstadoCitaRequest request) {
        return service.cambiarEstadoPorRecepcion(id, request.estado());
    }

    @PostMapping("/{id}/pago")
    public ResponseEntity<ComprobantePagoResponse> registrarPago(
            @AuthenticationPrincipal UsuarioPrincipal usuario,
            @PathVariable @Positive Long id,
            @Valid @RequestBody RegistrarPagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrarPagoPresencial(id, usuario.getId(), request.metodo(), request.monto()));
    }
}
