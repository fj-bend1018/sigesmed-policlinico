package com.sigesmed.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.dto.administracion.HorarioAdminResponse;
import com.sigesmed.api.dto.cita.CambiarEstadoCitaRequest;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaManagementService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/doctor")
public class DoctorPortalController {
    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final CitaManagementService citaManagementService;
    private final Clock clock;

    public DoctorPortalController(DoctorRepository doctorRepository, CitaRepository citaRepository,
            HorarioRepository horarioRepository, CitaManagementService citaManagementService, Clock clock) {
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.citaManagementService = citaManagementService;
        this.clock = clock;
    }

    public record DoctorPerfilResumen(Long id, String nombre, String cmp, String correo, String imagen,
            List<String> especialidades) { }

    @GetMapping("/resumen")
    @Transactional(readOnly = true)
    public DoctorPerfilResumen resumen(@AuthenticationPrincipal UsuarioPrincipal principal) {
        Doctor doctor = currentDoctor(principal);
        List<String> specs = doctor.getEspecialidades().stream()
                .map(Especialidad::getNombre)
                .sorted()
                .toList();
        return new DoctorPerfilResumen(doctor.getId(), doctor.getNombre(), doctor.getCmp(),
                doctor.getCorreo(), doctor.getImagen(), specs);
    }

    @GetMapping("/citas")
    @Transactional(readOnly = true)
    public List<CitaResponse> citas(@AuthenticationPrincipal UsuarioPrincipal principal,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        Doctor doctor = currentDoctor(principal);
        return citaRepository.findByDoctor(doctor.getId(), estado, fecha).stream()
                .map(CitaResponse::from)
                .toList();
    }

    @GetMapping("/horarios")
    @Transactional(readOnly = true)
    public List<HorarioAdminResponse> horarios(@AuthenticationPrincipal UsuarioPrincipal principal) {
        Doctor doctor = currentDoctor(principal);
        LocalDate hoy = LocalDate.now(clock);
        return horarioRepository.findByDoctor_IdAndFechaBetweenOrderByFechaAscHoraAsc(
                        doctor.getId(), hoy, hoy.plusDays(30)).stream()
                .map(h -> new HorarioAdminResponse(h.getId(), doctor.getId(), doctor.getNombre(),
                        null, null, h.getFecha(), h.getHora(), h.isDisponible(), h.getEstado()))
                .toList();
    }

    @PatchMapping("/citas/{id}/estado")
    @Transactional
    public CitaResponse cambiarEstado(@AuthenticationPrincipal UsuarioPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody CambiarEstadoCitaRequest request) {
        if (request.estado() != EstadoCita.ATENDIDA && request.estado() != EstadoCita.NO_ASISTIO) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El médico solo puede marcar la cita como ATENDIDA o NO_ASISTIO");
        }
        Doctor doctor = currentDoctor(principal);
        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        if (cita.getHorario() == null || cita.getHorario().getDoctor() == null
                || !cita.getHorario().getDoctor().getId().equals(doctor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta cita no pertenece a tu agenda");
        }
        return citaManagementService.cambiarEstadoPorRecepcion(id, request.estado());
    }

    private Doctor currentDoctor(UsuarioPrincipal principal) {
        return doctorRepository.findByUsuario_Id(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil médico no vinculado"));
    }
}
