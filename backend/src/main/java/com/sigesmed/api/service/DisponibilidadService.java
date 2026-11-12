package com.sigesmed.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.dto.disponibilidad.DoctorResponse;
import com.sigesmed.api.dto.disponibilidad.EspecialidadResponse;
import com.sigesmed.api.dto.disponibilidad.HorarioResponse;
import com.sigesmed.api.dto.disponibilidad.ServicioComplementarioResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;

@Service
public class DisponibilidadService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final EspecialidadRepository especialidadRepository;
    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;
    private final ServicioComplementarioRepository servicioRepository;

    @Autowired
    public DisponibilidadService(EspecialidadRepository especialidadRepository,
            DoctorRepository doctorRepository, HorarioRepository horarioRepository,
            CitaRepository citaRepository, ServicioComplementarioRepository servicioRepository, Clock clock) {
        this.especialidadRepository = especialidadRepository;
        this.doctorRepository = doctorRepository;
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.servicioRepository = servicioRepository;
        this.clock = clock;
    }

    public DisponibilidadService(EspecialidadRepository especialidadRepository,
            DoctorRepository doctorRepository, HorarioRepository horarioRepository,
            CitaRepository citaRepository, Clock clock) {
        this(especialidadRepository, doctorRepository, horarioRepository, citaRepository, null, clock);
    }

    @Transactional(readOnly = true)
    public List<EspecialidadResponse> especialidades() {
        return especialidadRepository.findAllByOrderByNombreAsc().stream()
                .map(item -> new EspecialidadResponse(item.getId(), item.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<ServicioComplementarioResponse> serviciosComplementarios() {
        return servicioRepository.findAllByOrderByNombreAsc().stream()
                .map(item -> new ServicioComplementarioResponse(item.getId(), item.getNombre())).toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> medicos(Long especialidadId) {
        if (!especialidadRepository.existsById(especialidadId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Especialidad no encontrada");
        }
        return doctorRepository.findActiveBySpecialty(especialidadId).stream().map(this::doctorDto).toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> horarios(Long doctorId, LocalDate fecha, Long usuarioId) {
        LocalDate today = LocalDate.now(clock.withZone(LIMA));
        if (fecha.isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha debe ser hoy o futura");
        }
        Doctor doctor = doctorRepository.findByIdAndEstado(doctorId, "ACTIVO")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Médico no encontrado o inactivo"));
        List<LocalTime> patientActiveTimes = usuarioId == null ? List.of()
                : citaRepository.findActiveTimesForUserAndDate(usuarioId, fecha, EstadoCita.ACTIVAS);
        List<HorarioResponse> slots = horarioRepository
                .findAvailableByDoctorAndDate(doctorId, fecha, EstadoHorario.DISPONIBLE, EstadoCita.ACTIVAS)
                .stream()
                .filter(slot -> !fecha.equals(today) || slot.getHora().isAfter(LocalTime.now(clock.withZone(LIMA))))
                .filter(slot -> !patientActiveTimes.contains(slot.getHora()))
                .map(slot -> new HorarioResponse(slot.getId(), doctor.getId(), doctor.getNombre(),
                        null, null, slot.getFecha(), slot.getHora()))
                .toList();
        return slots;
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> horariosServicio(Long servicioId, LocalDate fecha, Long usuarioId) {
        LocalDate today = LocalDate.now(clock.withZone(LIMA));
        if (fecha.isBefore(today)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha debe ser hoy o futura");
        }
        ServicioComplementario servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Servicio complementario no encontrado"));
        List<LocalTime> patientActiveTimes = usuarioId == null ? List.of()
                : citaRepository.findActiveTimesForUserAndDate(usuarioId, fecha, EstadoCita.ACTIVAS);
        return horarioRepository.findAvailableByServiceAndDate(servicioId, fecha,
                        EstadoHorario.DISPONIBLE, EstadoCita.ACTIVAS)
                .stream()
                .filter(slot -> !fecha.equals(today) || slot.getHora().isAfter(LocalTime.now(clock.withZone(LIMA))))
                .filter(slot -> !patientActiveTimes.contains(slot.getHora()))
                .map(slot -> new HorarioResponse(slot.getId(), null, null, servicio.getId(), servicio.getNombre(),
                        slot.getFecha(), slot.getHora()))
                .toList();
    }

    private DoctorResponse doctorDto(Doctor doctor) {
        List<String> specialties = doctor.getEspecialidades().stream()
                .map(Especialidad::getNombre).sorted().toList();
        return new DoctorResponse(doctor.getId(), doctor.getNombre(), doctor.getCmp(), specialties);
    }
}
