package com.sigesmed.api.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.dto.administracion.CambiarEstadoRequest;
import com.sigesmed.api.dto.administracion.DoctorAdminRequest;
import com.sigesmed.api.dto.administracion.DoctorAdminResponse;
import com.sigesmed.api.dto.administracion.HorarioAdminRequest;
import com.sigesmed.api.dto.administracion.HorarioAdminResponse;
import com.sigesmed.api.dto.administracion.HorarioRecurrenteRequest;
import com.sigesmed.api.dto.administracion.HorarioRecurrenteResponse;
import com.sigesmed.api.dto.administracion.RecepcionistaActualizarRequest;
import com.sigesmed.api.dto.administracion.RecepcionistaAdminResponse;
import com.sigesmed.api.dto.administracion.RecepcionistaCrearRequest;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;

@Service
public class AdministracionService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final DoctorRepository doctorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final HorarioRepository horarioRepository;
    private final CitaRepository citaRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ServicioComplementarioRepository servicioRepository;

    @Autowired
    public AdministracionService(DoctorRepository doctorRepository, EspecialidadRepository especialidadRepository,
            HorarioRepository horarioRepository, CitaRepository citaRepository,
            RecepcionistaRepository recepcionistaRepository, UsuarioRepository usuarioRepository,
            ServicioComplementarioRepository servicioRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.doctorRepository = doctorRepository;
        this.especialidadRepository = especialidadRepository;
        this.horarioRepository = horarioRepository;
        this.citaRepository = citaRepository;
        this.recepcionistaRepository = recepcionistaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.servicioRepository = servicioRepository;
    }

    public AdministracionService(DoctorRepository doctorRepository, EspecialidadRepository especialidadRepository,
            HorarioRepository horarioRepository, CitaRepository citaRepository,
            RecepcionistaRepository recepcionistaRepository, UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder, Clock clock) {
        this(doctorRepository, especialidadRepository, horarioRepository, citaRepository, recepcionistaRepository,
                usuarioRepository, null, passwordEncoder, clock);
    }

    @Transactional(readOnly = true)
    public List<DoctorAdminResponse> listarDoctores() {
        return doctorRepository.findAllByOrderByNombreAsc().stream().map(this::doctorResponse).toList();
    }

    @Transactional(readOnly = true)
    public DoctorAdminResponse obtenerDoctor(Long id) {
        return doctorResponse(doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Médico no encontrado")));
    }

    @Transactional
    public DoctorAdminResponse crearDoctor(DoctorAdminRequest request) {
        String cmp = request.cmp().trim().toUpperCase(Locale.ROOT);
        if (doctorRepository.existsByCmpIgnoreCase(cmp)) {
            throw new ApiException(HttpStatus.CONFLICT, "El CMP ya está registrado");
        }
        Doctor doctor = new Doctor(request.nombre().trim(), cmp, especialidades(request.especialidadIds()));
        try {
            return doctorResponse(doctorRepository.saveAndFlush(doctor));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El médico o CMP ya está registrado");
        }
    }

    @Transactional
    public DoctorAdminResponse actualizarDoctor(Long id, DoctorAdminRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        String cmp = request.cmp().trim().toUpperCase(Locale.ROOT);
        if (doctorRepository.existsByCmpIgnoreCaseAndIdNot(cmp, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "El CMP ya está registrado");
        }
        doctor.actualizar(request.nombre().trim(), cmp, especialidades(request.especialidadIds()));
        try {
            return doctorResponse(doctorRepository.saveAndFlush(doctor));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El médico o CMP ya está registrado");
        }
    }

    @Transactional
    public DoctorAdminResponse cambiarEstadoDoctor(Long id, String estado) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Médico no encontrado"));
        doctor.cambiarEstado(estado);
        return doctorResponse(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<HorarioAdminResponse> listarHorarios(Long doctorId, LocalDate desde, LocalDate hasta) {
        return listarHorarios(doctorId, null, desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<HorarioAdminResponse> listarHorarios(Long doctorId, Long servicioId, LocalDate desde, LocalDate hasta) {
        LocalDate start = desde == null ? LocalDate.now(clock) : desde;
        LocalDate end = hasta == null ? start.plusDays(30) : hasta;
        if (end.isBefore(start) || end.isAfter(start.plusDays(366))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rango de fechas no es válido (máximo 366 días)");
        }
        if (doctorId != null && servicioId != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Filtra los horarios por un solo recurso");
        }
        List<Horario> items = doctorId != null
                ? horarioRepository.findByDoctor_IdAndFechaBetweenOrderByFechaAscHoraAsc(doctorId, start, end)
                : servicioId != null
                    ? horarioRepository.findByServicioComplementario_IdAndFechaBetweenOrderByFechaAscHoraAsc(servicioId, start, end)
                    : horarioRepository.findAllByFechaBetweenOrderByFechaAscHoraAsc(start, end);
        return items.stream().map(this::horarioResponse).toList();
    }

    @Transactional(readOnly = true)
    public HorarioAdminResponse obtenerHorario(Long id) {
        return horarioResponse(horarioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Horario no encontrado")));
    }

    @Transactional
    public HorarioAdminResponse crearHorario(HorarioAdminRequest request) {
        validarRecurso(request);
        validarFechaHora(request.fecha(), request.hora());
        validarHorarioNoDuplicado(request, null);
        Horario horario = nuevoHorario(request);
        try {
            return horarioResponse(horarioRepository.saveAndFlush(horario));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un horario para ese recurso y momento");
        }
    }

    @Transactional
    public HorarioRecurrenteResponse crearHorariosRecurrentes(HorarioRecurrenteRequest request) {
        if (request.fechaHasta().isBefore(request.fechaDesde())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha final debe ser igual o posterior a la inicial");
        }
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La hora de inicio debe ser menor a la hora de fin");
        }
        Doctor doctor = doctorActivo(request.doctorId());
        Set<DayOfWeek> dias = Set.copyOf(request.diasSemana());
        Set<LocalDate> descansos = request.diasDescanso() == null ? Set.of() : Set.copyOf(request.diasDescanso());
        LocalDate today = LocalDate.now(clock.withZone(LIMA));
        LocalTime now = LocalTime.now(clock.withZone(LIMA));

        List<Horario> porGuardar = new ArrayList<>();
        for (LocalDate fecha = request.fechaDesde(); !fecha.isAfter(request.fechaHasta()); fecha = fecha.plusDays(1)) {
            if (!dias.contains(fecha.getDayOfWeek()) || descansos.contains(fecha) || fecha.isBefore(today)) {
                continue;
            }
            for (LocalTime hora = request.horaInicio(); hora.isBefore(request.horaFin()); hora = hora.plusHours(1)) {
                if (fecha.equals(today) && !hora.isAfter(now)) {
                    continue;
                }
                if (horarioRepository.existsByDoctor_IdAndFechaAndHora(doctor.getId(), fecha, hora)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Ya existe un horario para ese recurso y momento");
                }
                porGuardar.add(new Horario(doctor, fecha, hora));
            }
        }
        if (porGuardar.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No hay franjas futuras válidas en el rango seleccionado");
        }
        try {
            List<Horario> guardados = horarioRepository.saveAllAndFlush(porGuardar);
            return new HorarioRecurrenteResponse(doctor.getId(), doctor.getNombre(), guardados.size(),
                    guardados.stream().map(this::horarioResponse).toList());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un horario para ese recurso y momento");
        }
    }

    @Transactional
    public HorarioAdminResponse actualizarHorario(Long id, HorarioAdminRequest request) {
        validarRecurso(request);
        validarFechaHora(request.fecha(), request.hora());
        Horario horario = horarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Horario no encontrado"));
        if (!horario.isDisponible() || citaRepository.existsByHorario_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede modificar un horario reservado o con historial");
        }
        validarHorarioNoDuplicado(request, id);
        Doctor doctor = request.doctorId() == null ? null : doctorActivo(request.doctorId());
        ServicioComplementario servicio = servicioComplementario(request.servicioId());
        if (doctor != null) horario.actualizar(doctor, request.fecha(), request.hora());
        else horario.actualizar(servicio, request.fecha(), request.hora());
        try {
            return horarioResponse(horarioRepository.saveAndFlush(horario));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un horario para ese recurso y momento");
        }
    }

    @Transactional
    public void eliminarHorario(Long id) {
        Horario horario = horarioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Horario no encontrado"));
        if (citaRepository.existsByHorario_IdAndEstadoIn(id, EstadoCita.ACTIVAS)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar el horario porque tiene una cita reservada o confirmada");
        }
        if (!horario.isDisponible()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar el horario mientras figure como ocupado");
        }
        if (citaRepository.existsByHorario_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar el horario porque conserva citas históricas vinculadas");
        }
        horarioRepository.delete(horario);
    }

    @Transactional(readOnly = true)
    public List<RecepcionistaAdminResponse> listarRecepcionistas() {
        return recepcionistaRepository.findAllByOrderByApellidoPaternoAscNombreAsc().stream()
                .map(this::recepcionistaResponse).toList();
    }

    @Transactional(readOnly = true)
    public RecepcionistaAdminResponse obtenerRecepcionista(Long id) {
        return recepcionistaResponse(recepcionistaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado")));
    }

    @Transactional
    public RecepcionistaAdminResponse crearRecepcionista(RecepcionistaCrearRequest request) {
        String dni = request.dni().trim();
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByDni(dni) || recepcionistaRepository.existsByDni(dni)) {
            throw new ApiException(HttpStatus.CONFLICT, "El DNI ya está registrado");
        }
        if (!usuarioRepository.findByIdentifier(correo).isEmpty() || recepcionistaRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta");
        }
        Usuario usuario = usuarioRepository.save(new Usuario(dni, passwordEncoder.encode(request.claveTemporal()),
                RolUsuario.RECEPCIONISTA));
        Recepcionista persona = new Recepcionista(usuario, request.nombre().trim(), request.apellidoPaterno().trim(),
                blankToNull(request.apellidoMaterno()), dni, correo);
        try {
            persona.actualizar(persona.getNombre(), persona.getApellidoPaterno(), persona.getApellidoMaterno(), correo,
                    blankToNull(request.celular()), blankToNull(request.direccion()));
            return recepcionistaResponse(recepcionistaRepository.saveAndFlush(persona));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El DNI o correo ya está registrado");
        }
    }

    @Transactional
    public RecepcionistaAdminResponse actualizarRecepcionista(Long id, RecepcionistaActualizarRequest request) {
        Recepcionista persona = recepcionistaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado"));
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (recepcionistaRepository.existsByCorreoIgnoreCaseAndIdNot(correo, id)
                || usuarioRepository.findByIdentifier(correo).stream()
                        .anyMatch(usuario -> !usuario.getId().equals(persona.getUsuario().getId()))) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta");
        }
        persona.actualizar(request.nombre().trim(), request.apellidoPaterno().trim(),
                blankToNull(request.apellidoMaterno()), correo, blankToNull(request.celular()),
                blankToNull(request.direccion()));
        try {
            return recepcionistaResponse(recepcionistaRepository.saveAndFlush(persona));
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
    }

    @Transactional
    public RecepcionistaAdminResponse cambiarEstadoRecepcionista(Long id, String estado) {
        Recepcionista persona = recepcionistaRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Recepcionista no encontrado"));
        persona.cambiarEstado(estado);
        return recepcionistaResponse(recepcionistaRepository.save(persona));
    }

    private Set<Especialidad> especialidades(List<Long> ids) {
        Set<Long> uniqueIds = new HashSet<>(ids);
        List<Especialidad> items = especialidadRepository.findAllById(uniqueIds);
        if (items.size() != uniqueIds.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Una o más especialidades no existen");
        }
        return new HashSet<>(items);
    }

    private Doctor doctorActivo(Long id) {
        return doctorRepository.findByIdAndEstado(id, "ACTIVO")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Médico no encontrado o inactivo"));
    }

    private ServicioComplementario servicioComplementario(Long id) {
        if (id == null) return null;
        if (servicioRepository == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Servicio complementario no encontrado");
        }
        return servicioRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Servicio complementario no encontrado"));
    }

    private Horario nuevoHorario(HorarioAdminRequest request) {
        if (request.doctorId() != null) {
            return new Horario(doctorActivo(request.doctorId()), request.fecha(), request.hora());
        }
        return new Horario(servicioComplementario(request.servicioId()), request.fecha(), request.hora());
    }

    private void validarRecurso(HorarioAdminRequest request) {
        if ((request.doctorId() == null) == (request.servicioId() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Selecciona un médico o un servicio complementario, no ambos");
        }
    }

    private void validarHorarioNoDuplicado(HorarioAdminRequest request, Long exceptoId) {
        boolean duplicate;
        if (request.doctorId() != null) {
            Doctor doctor = doctorActivo(request.doctorId());
            duplicate = exceptoId == null
                    ? horarioRepository.existsByDoctor_IdAndFechaAndHora(doctor.getId(), request.fecha(), request.hora())
                    : horarioRepository.existsByDoctor_IdAndFechaAndHoraAndIdNot(doctor.getId(), request.fecha(), request.hora(), exceptoId);
        } else {
            ServicioComplementario servicio = servicioComplementario(request.servicioId());
            duplicate = exceptoId == null
                    ? horarioRepository.existsByServicioComplementario_IdAndFechaAndHora(servicio.getId(), request.fecha(), request.hora())
                    : horarioRepository.existsByServicioComplementario_IdAndFechaAndHoraAndIdNot(servicio.getId(), request.fecha(), request.hora(), exceptoId);
        }
        if (duplicate) throw new ApiException(HttpStatus.CONFLICT, "Ya existe un horario para ese recurso y momento");
    }

    private void validarFechaHora(LocalDate fecha, LocalTime hora) {
        LocalDate today = LocalDate.now(clock.withZone(LIMA));
        if (fecha.isBefore(today) || (fecha.equals(today) && !hora.isAfter(LocalTime.now(clock.withZone(LIMA))))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El horario debe ser futuro");
        }
    }

    private DoctorAdminResponse doctorResponse(Doctor doctor) {
        List<Especialidad> specialties = doctor.getEspecialidades().stream()
                .sorted(java.util.Comparator.comparing(Especialidad::getNombre)).toList();
        return new DoctorAdminResponse(doctor.getId(), doctor.getNombre(), doctor.getCmp(), doctor.getEstado(),
                specialties.stream().map(Especialidad::getId).toList(),
                specialties.stream().map(Especialidad::getNombre).toList());
    }

    private HorarioAdminResponse horarioResponse(Horario horario) {
        Doctor doctor = horario.getDoctor();
        ServicioComplementario servicio = horario.getServicioComplementario();
        return new HorarioAdminResponse(horario.getId(), doctor == null ? null : doctor.getId(),
                doctor == null ? null : doctor.getNombre(), servicio == null ? null : servicio.getId(),
                servicio == null ? null : servicio.getNombre(), horario.getFecha(), horario.getHora(),
                horario.isDisponible(), horario.getEstado());
    }

    private RecepcionistaAdminResponse recepcionistaResponse(Recepcionista persona) {
        return new RecepcionistaAdminResponse(persona.getId(), persona.getNombre(), persona.getApellidoPaterno(),
                persona.getApellidoMaterno(), persona.getDni(), persona.getCorreo(), persona.getCelular(),
                persona.getDireccion(), persona.getEstado());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
