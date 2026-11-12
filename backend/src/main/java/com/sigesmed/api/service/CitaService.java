package com.sigesmed.api.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;

@Service
public class CitaService {
    private final PacienteRepository pacienteRepository;
    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final EspecialidadRepository especialidadRepository;
    private final ServicioComplementarioRepository servicioRepository;
    private final Clock clock;

    @Autowired
    public CitaService(PacienteRepository pacienteRepository, HorarioRepository horarioRepository,
            CitaRepository citaRepository, EspecialidadRepository especialidadRepository,
            ServicioComplementarioRepository servicioRepository, Clock clock) {
        this.pacienteRepository = pacienteRepository;
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.especialidadRepository = especialidadRepository;
        this.servicioRepository = servicioRepository;
        this.clock = clock;
    }

    public CitaService(PacienteRepository pacienteRepository, HorarioRepository horarioRepository,
            CitaRepository citaRepository, EspecialidadRepository especialidadRepository, Clock clock) {
        this(pacienteRepository, horarioRepository, citaRepository, especialidadRepository, null, clock);
    }

    @Transactional
    public CitaResponse reservar(Long usuarioId, Long horarioId, Long especialidadId) {
        return reservar(usuarioId, horarioId, especialidadId, null);
    }

    @Transactional
    public CitaResponse reservar(Long usuarioId, Long horarioId, Long especialidadId, Long servicioId) {
        if ((especialidadId == null) == (servicioId == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona una especialidad o un servicio complementario");
        }
        Paciente paciente = pacienteRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil de paciente no encontrado"));
        Horario horario = horarioRepository.findByIdForUpdate(horarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Horario no encontrado"));
        if (!horario.isDisponible() || horario.getEstado() != EstadoHorario.DISPONIBLE
                || citaRepository.existsByHorario_IdAndEstadoIn(horarioId, EstadoCita.ACTIVAS)) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario ya no está disponible");
        }
        Especialidad especialidad = null;
        ServicioComplementario servicio = null;
        if (especialidadId != null) {
            especialidad = especialidadRepository.findById(especialidadId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "La especialidad no existe"));
            boolean doctorOffersSpecialty = horario.getDoctor() != null && horario.getDoctor().getEspecialidades().stream()
                    .anyMatch(item -> item.getId().equals(especialidadId));
            if (!doctorOffersSpecialty || horario.getServicioComplementario() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El médico no atiende la especialidad seleccionada");
            }
        } else {
            if (servicioRepository == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El servicio complementario no está disponible");
            }
            servicio = servicioRepository.findById(servicioId)
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El servicio complementario no existe"));
            if (horario.getServicioComplementario() == null
                    || !horario.getServicioComplementario().getId().equals(servicioId)
                    || horario.getDoctor() != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El horario no corresponde al servicio seleccionado");
            }
        }
        LocalDate today = LocalDate.now(clock);
        if (horario.getFecha().isBefore(today)
                || (horario.getFecha().equals(today) && !horario.getHora().isAfter(LocalTime.now(clock)))) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario ya no está disponible");
        }
        if (citaRepository.existsActiveForPatientAt(paciente.getId(), horario.getFecha(), horario.getHora(),
                EstadoCita.ACTIVAS)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tienes una cita activa en esa fecha y hora");
        }

        horario.ocupar();
        horarioRepository.save(horario);
        Cita cita = especialidad != null
                ? new Cita(paciente, horario, especialidad, EstadoCita.RESERVADA, OffsetDateTime.now(clock))
                : new Cita(paciente, horario, servicio, EstadoCita.RESERVADA, OffsetDateTime.now(clock));
        try {
            return toResponse(citaRepository.saveAndFlush(cita));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario acaba de ser reservado");
        }
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerPropia(Long usuarioId, Long citaId) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil de paciente no encontrado"));
        Cita cita = citaRepository.findByIdAndPaciente_Id(citaId, paciente.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return toResponse(cita);
    }

    private CitaResponse toResponse(Cita cita) {
        return CitaResponse.from(cita);
    }
}
