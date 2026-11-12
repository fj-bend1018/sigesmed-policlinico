package com.sigesmed.api.controller;

import java.time.LocalDate;
import java.util.List;

import com.sigesmed.api.dto.administracion.CambiarEstadoRequest;
import com.sigesmed.api.dto.administracion.DoctorAdminRequest;
import com.sigesmed.api.dto.administracion.DoctorAdminResponse;
import com.sigesmed.api.dto.administracion.HorarioAdminRequest;
import com.sigesmed.api.dto.administracion.HorarioAdminResponse;
import com.sigesmed.api.dto.administracion.RecepcionistaActualizarRequest;
import com.sigesmed.api.dto.administracion.RecepcionistaAdminResponse;
import com.sigesmed.api.dto.administracion.RecepcionistaCrearRequest;
import com.sigesmed.api.service.AdministracionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/administrador")
public class AdministracionController {
    private final AdministracionService service;

    public AdministracionController(AdministracionService service) { this.service = service; }

    @GetMapping("/doctores")
    public List<DoctorAdminResponse> doctores() { return service.listarDoctores(); }

    @GetMapping("/doctores/{id}")
    public DoctorAdminResponse doctor(@PathVariable @Positive Long id) { return service.obtenerDoctor(id); }

    @PostMapping("/doctores")
    public ResponseEntity<DoctorAdminResponse> crearDoctor(@Valid @RequestBody DoctorAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearDoctor(request));
    }

    @PutMapping("/doctores/{id}")
    public DoctorAdminResponse actualizarDoctor(@PathVariable @Positive Long id,
            @Valid @RequestBody DoctorAdminRequest request) {
        return service.actualizarDoctor(id, request);
    }

    @PatchMapping("/doctores/{id}/estado")
    public DoctorAdminResponse estadoDoctor(@PathVariable @Positive Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return service.cambiarEstadoDoctor(id, request.estado());
    }

    @GetMapping("/horarios")
    public List<HorarioAdminResponse> horarios(@RequestParam(required = false) @Positive Long doctorId,
            @RequestParam(required = false) @Positive Long servicioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listarHorarios(doctorId, servicioId, desde, hasta);
    }

    @GetMapping("/horarios/{id}")
    public HorarioAdminResponse horario(@PathVariable @Positive Long id) { return service.obtenerHorario(id); }

    @PostMapping("/horarios")
    public ResponseEntity<HorarioAdminResponse> crearHorario(@Valid @RequestBody HorarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearHorario(request));
    }

    @PutMapping("/horarios/{id}")
    public HorarioAdminResponse actualizarHorario(@PathVariable @Positive Long id,
            @Valid @RequestBody HorarioAdminRequest request) {
        return service.actualizarHorario(id, request);
    }

    @DeleteMapping("/horarios/{id}")
    public ResponseEntity<Void> eliminarHorario(@PathVariable @Positive Long id) {
        service.eliminarHorario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recepcionistas")
    public List<RecepcionistaAdminResponse> recepcionistas() { return service.listarRecepcionistas(); }

    @GetMapping("/recepcionistas/{id}")
    public RecepcionistaAdminResponse recepcionista(@PathVariable @Positive Long id) {
        return service.obtenerRecepcionista(id);
    }

    @PostMapping("/recepcionistas")
    public ResponseEntity<RecepcionistaAdminResponse> crearRecepcionista(
            @Valid @RequestBody RecepcionistaCrearRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearRecepcionista(request));
    }

    @PutMapping("/recepcionistas/{id}")
    public RecepcionistaAdminResponse actualizarRecepcionista(@PathVariable @Positive Long id,
            @Valid @RequestBody RecepcionistaActualizarRequest request) {
        return service.actualizarRecepcionista(id, request);
    }

    @PatchMapping("/recepcionistas/{id}/estado")
    public RecepcionistaAdminResponse estadoRecepcionista(@PathVariable @Positive Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return service.cambiarEstadoRecepcionista(id, request.estado());
    }
}
