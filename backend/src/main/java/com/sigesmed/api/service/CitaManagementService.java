package com.sigesmed.api.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Pago;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.domain.enums.EstadoPago;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.dto.cita.ComprobantePagoResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.PagoRepository;
import com.sigesmed.api.repository.UsuarioRepository;

@Service
public class CitaManagementService {
    private static final Set<EstadoCita> PATIENT_ACTIVE = Set.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA);
    private static final Set<EstadoCita> HISTORY = Set.of(EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO);
    private static final Set<EstadoCita> CANCELLED = Set.of(EstadoCita.CANCELADA);

    private final PacienteRepository pacienteRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final PagoRepository pagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final Duration plazoCancelacion;

    public CitaManagementService(PacienteRepository pacienteRepository, CitaRepository citaRepository,
            HorarioRepository horarioRepository, Clock clock,
            @Value("${sigesmed.cita.plazo-cancelacion:PT2H}") Duration plazoCancelacion) {
        this(pacienteRepository, citaRepository, horarioRepository, null, null, clock, plazoCancelacion);
    }

    @Autowired
    public CitaManagementService(PacienteRepository pacienteRepository, CitaRepository citaRepository,
            HorarioRepository horarioRepository, PagoRepository pagoRepository,
            UsuarioRepository usuarioRepository, Clock clock,
            @Value("${sigesmed.cita.plazo-cancelacion:PT2H}") Duration plazoCancelacion) {
        if (plazoCancelacion == null || plazoCancelacion.isNegative() || plazoCancelacion.isZero()) {
            throw new IllegalArgumentException("sigesmed.cita.plazo-cancelacion debe ser positivo");
        }
        this.pacienteRepository = pacienteRepository;
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.pagoRepository = pagoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.plazoCancelacion = plazoCancelacion;
    }

    @Transactional
    public ComprobantePagoResponse registrarPagoPresencial(Long citaId, Long usuarioId, String metodo, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor a cero");
        }
        if (metodo == null || metodo.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un método de pago");
        }
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        if (cita.getPago() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "La cita ya tiene un pago registrado");
        }
        if (cita.getEstado() != EstadoCita.RESERVADA) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden pagar citas en estado RESERVADA");
        }
        Usuario recepcionista = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Pago pago = pagoRepository.save(new Pago(
                metodo.trim().toUpperCase(Locale.ROOT), monto, EstadoPago.PAGADO, recepcionista));
        cita.confirmarConPago(pago);
        citaRepository.save(cita);
        return ComprobantePagoResponse.from(cita, pago, recepcionista.getDni());
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarPropias(Long usuarioId, String vista) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil de paciente no encontrado"));
        Collection<EstadoCita> estados = switch (vista == null ? "PROXIMAS" : vista.toUpperCase(Locale.ROOT)) {
            case "PROXIMAS" -> PATIENT_ACTIVE;
            case "HISTORIAL" -> HISTORY;
            case "CANCELADAS" -> CANCELLED;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "La vista de citas no es válida");
        };
        return citaRepository.findPatientCitas(paciente.getId(), estados).stream().map(CitaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarAgenda(EstadoCita estado, LocalDate fecha, String busqueda) {
        List<Cita> citas = citaRepository.findAgenda(estado, fecha);
        if (busqueda == null || busqueda.isBlank()) return citas.stream().map(CitaResponse::from).toList();
        String query = busqueda.trim().toLowerCase(Locale.ROOT);
        return citas.stream().filter(cita -> matches(cita, query)).map(CitaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerParaRecepcion(Long citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return CitaResponse.from(cita);
    }

    @Transactional
    public CitaResponse cancelarPropia(Long usuarioId, Long citaId, String motivo) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil de paciente no encontrado"));
        Cita cita = citaRepository.findByIdAndPaciente_IdForUpdate(citaId, paciente.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        verificarAnticipacion(cita);
        return cancelarBloqueada(cita, motivo);
    }

    @Transactional
    public CitaResponse cancelarPorRecepcion(Long citaId, String motivo) {
        return cancelar(citaId, motivo, true);
    }

    @Transactional
    public CitaResponse reprogramarPropia(Long usuarioId, Long citaId, Long horarioId) {
        Paciente paciente = pacienteRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Perfil de paciente no encontrado"));
        Cita cita = citaRepository.findByIdAndPaciente_IdForUpdate(citaId, paciente.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return reprogramar(cita, horarioId);
    }

    @Transactional
    public CitaResponse reprogramarPorRecepcion(Long citaId, Long horarioId) {
        return reprogramar(citaId, horarioId);
    }

    @Transactional
    public CitaResponse cambiarEstadoPorRecepcion(Long citaId, EstadoCita nuevoEstado) {
        Cita cita = citaRepository.findByIdForUpdate(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        if (nuevoEstado == EstadoCita.CANCELADA) {
            verificarAnticipacion(cita);
            return cancelarBloqueada(cita, null);
        }
        if (nuevoEstado != EstadoCita.ATENDIDA && nuevoEstado != EstadoCita.NO_ASISTIO) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Recepción solo puede marcar una cita como atendida, inasistencia o cancelada");
        }
        if (!PATIENT_ACTIVE.contains(cita.getEstado())) {
            throw new ApiException(HttpStatus.CONFLICT, "La cita ya no está activa");
        }
        if (instante(cita.getHorario()).isAfter(clock.instant())) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede cerrar una cita antes de su horario");
        }
        cita.setEstado(nuevoEstado);
        return CitaResponse.from(citaRepository.save(cita));
    }

    private CitaResponse cancelar(Long citaId, String motivo, boolean validarAnticipacion) {
        Cita cita = citaRepository.findByIdForUpdate(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        if (validarAnticipacion) verificarAnticipacion(cita);
        return cancelarBloqueada(cita, motivo);
    }

    private CitaResponse cancelarBloqueada(Cita cita, String motivo) {
        if (cita.getEstado() != EstadoCita.RESERVADA) {
            if (cita.getEstado() == EstadoCita.CONFIRMADA) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "La cita está confirmada con pago; la cancelación requiere una política de devolución");
            }
            throw new ApiException(HttpStatus.CONFLICT, "La cita ya no se puede cancelar");
        }
        Horario horario = horarioRepository.findByIdForUpdate(cita.getHorario().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El horario asociado ya no existe"));
        horario.liberar();
        cita.cancelar(blankToNull(motivo));
        horarioRepository.save(horario);
        return CitaResponse.from(citaRepository.save(cita));
    }

    private CitaResponse reprogramar(Long citaId, Long horarioId) {
        Cita cita = citaRepository.findByIdForUpdate(citaId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
        return reprogramar(cita, horarioId);
    }

    private CitaResponse reprogramar(Cita cita, Long horarioId) {
        if (cita.getEstado() != EstadoCita.RESERVADA) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Solo se pueden reprogramar citas reservadas sin pago confirmado");
        }
        verificarAnticipacion(cita);
        if (cita.getHorario().getId().equals(horarioId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecciona un horario distinto al actual");
        }
        Horario nuevo = horarioRepository.findByIdForUpdate(horarioId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Horario nuevo no encontrado"));
        if (cita.getServicioComplementario() != null) {
            if (nuevo.getServicioComplementario() == null
                    || !nuevo.getServicioComplementario().getId().equals(cita.getServicioComplementario().getId())
                    || nuevo.getDoctor() != null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "La reprogramación debe conservar el servicio complementario de la cita");
            }
        } else {
            if (cita.getEspecialidad() == null || nuevo.getDoctor() == null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "La cita no tiene una especialidad confirmada; no se puede reprogramar");
            }
            boolean mismaEspecialidad = nuevo.getDoctor().getEspecialidades().stream()
                    .anyMatch(especialidad -> especialidad.getId().equals(cita.getEspecialidad().getId()));
            if (!mismaEspecialidad || nuevo.getServicioComplementario() != null) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "La reprogramación debe conservar la especialidad de la cita");
            }
        }
        if (!nuevo.isDisponible() || nuevo.getEstado() != EstadoHorario.DISPONIBLE) {
            throw new ApiException(HttpStatus.CONFLICT, "El horario elegido ya no está disponible");
        }
        if (!instante(nuevo).isAfter(clock.instant())) {
            throw new ApiException(HttpStatus.CONFLICT, "El nuevo horario debe estar en el futuro");
        }
        if (citaRepository.existsActiveForPatientAt(cita.getPaciente().getId(), nuevo.getFecha(), nuevo.getHora(),
                PATIENT_ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "El paciente ya tiene una cita activa en esa fecha y hora");
        }

        Horario anterior = horarioRepository.findByIdForUpdate(cita.getHorario().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "El horario actual ya no existe"));
        anterior.liberar();
        nuevo.ocupar();
        cita.setHorario(nuevo);
        horarioRepository.save(anterior);
        horarioRepository.save(nuevo);
        try {
            return CitaResponse.from(citaRepository.saveAndFlush(cita));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "No se pudo reservar el nuevo horario");
        }
    }

    private void verificarAnticipacion(Cita cita) {
        if (instante(cita.getHorario()).isBefore(clock.instant().plus(plazoCancelacion))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Solo puedes cancelar o reprogramar con al menos " + plazoCancelacion.toHours()
                            + " horas de anticipación");
        }
    }

    private Instant instante(Horario horario) {
        return LocalDateTime.of(horario.getFecha(), horario.getHora()).atZone(clock.getZone()).toInstant();
    }

    private boolean matches(Cita cita, String query) {
        String name = cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidoPaterno()
                + " " + String.valueOf(cita.getPaciente().getApellidoMaterno());
        if (cita.getServicioComplementario() != null) {
            return contains(name, query) || contains(cita.getPaciente().getDni(), query)
                    || contains(cita.getPaciente().getCorreo(), query)
                    || contains(cita.getServicioComplementario().getNombre(), query);
        }
        return contains(name, query) || contains(cita.getPaciente().getDni(), query)
                || contains(cita.getPaciente().getCorreo(), query)
                || contains(cita.getHorario().getDoctor().getNombre(), query)
                || cita.getHorario().getDoctor().getEspecialidades().stream()
                        .anyMatch(especialidad -> contains(especialidad.getNombre(), query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
