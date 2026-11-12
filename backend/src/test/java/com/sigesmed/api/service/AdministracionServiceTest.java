package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.administracion.DoctorAdminRequest;
import com.sigesmed.api.dto.administracion.HorarioAdminRequest;
import com.sigesmed.api.dto.administracion.RecepcionistaActualizarRequest;
import com.sigesmed.api.dto.administracion.RecepcionistaCrearRequest;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdministracionServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-08T15:00:00Z"), LIMA);

    @Mock private DoctorRepository doctores;
    @Mock private EspecialidadRepository especialidades;
    @Mock private HorarioRepository horarios;
    @Mock private CitaRepository citas;
    @Mock private RecepcionistaRepository recepcionistas;
    @Mock private UsuarioRepository usuarios;
    @Mock private ServicioComplementarioRepository servicios;
    @Mock private PasswordEncoder encoder;

    private AdministracionService service;

    @BeforeEach
    void setUp() {
        service = new AdministracionService(doctores, especialidades, horarios, citas, recepcionistas,
                usuarios, servicios, encoder, clock);
    }

    // === DOCTOR ===

    @Test
    void listarDoctores_retornaListaMapeada() {
        Doctor doc = new Doctor("Dra. Ana Demo", "CMP-001", Set.of());
        ReflectionTestUtils.setField(doc, "id", 1L);
        when(doctores.findAllByOrderByNombreAsc()).thenReturn(List.of(doc));

        var result = service.listarDoctores();
        assertEquals(1, result.size());
        assertEquals("Dra. Ana Demo", result.getFirst().nombre());
    }

    @Test
    void obtenerDoctor_noExisteLanza404() {
        when(doctores.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.obtenerDoctor(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void obtenerDoctor_exitoso() {
        Especialidad esp = new Especialidad("Pediatría");
        ReflectionTestUtils.setField(esp, "id", 10L);
        Doctor doc = new Doctor("Dr. Juan Demo", "CMP-002", Set.of(esp));
        ReflectionTestUtils.setField(doc, "id", 2L);
        when(doctores.findById(2L)).thenReturn(Optional.of(doc));

        var resp = service.obtenerDoctor(2L);
        assertEquals(2L, resp.id());
        assertEquals("CMP-002", resp.cmp());
        assertEquals(List.of("Pediatría"), resp.especialidades());
    }

    @Test
    void crearDoctor_cmpDuplicadoLanza409() {
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Nuevo", "CMP-DUPLICADO", List.of(10L));
        when(doctores.existsByCmpIgnoreCase("CMP-DUPLICADO")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.crearDoctor(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El CMP ya está registrado", ex.getMessage());
        verify(doctores, never()).saveAndFlush(any());
    }

    @Test
    void crearDoctor_especialidadInvalidaLanza400() {
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Nuevo", "CMP-999", List.of(10L, 20L));
        when(doctores.existsByCmpIgnoreCase("CMP-999")).thenReturn(false);
        when(especialidades.findAllById(any())).thenReturn(List.of(new Especialidad("Pediatría")));

        ApiException ex = assertThrows(ApiException.class, () -> service.crearDoctor(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Una o más especialidades no existen", ex.getMessage());
    }

    @Test
    void crearDoctor_exitoso() {
        Especialidad esp = new Especialidad("Pediatría");
        ReflectionTestUtils.setField(esp, "id", 10L);
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Nuevo", "CMP-999", List.of(10L));
        when(doctores.existsByCmpIgnoreCase("CMP-999")).thenReturn(false);
        when(especialidades.findAllById(any())).thenReturn(List.of(esp));
        when(doctores.saveAndFlush(any(Doctor.class))).thenAnswer(i -> {
            Doctor d = i.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 5L);
            return d;
        });

        var resp = service.crearDoctor(request);
        assertEquals(5L, resp.id());
        assertEquals("Dr. Nuevo", resp.nombre());
        assertEquals("CMP-999", resp.cmp());
    }

    @Test
    void actualizarDoctor_noExisteLanza404() {
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Modificado", "CMP-888", List.of(10L));
        when(doctores.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizarDoctor(99L, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void actualizarDoctor_cmpDuplicadoEnOtroLanza409() {
        Doctor doc = new Doctor("Dr. Antiguo", "CMP-OLD", Set.of());
        ReflectionTestUtils.setField(doc, "id", 3L);
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Modificado", "CMP-DUPLICADO", List.of(10L));
        when(doctores.findById(3L)).thenReturn(Optional.of(doc));
        when(doctores.existsByCmpIgnoreCaseAndIdNot("CMP-DUPLICADO", 3L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizarDoctor(3L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El CMP ya está registrado", ex.getMessage());
    }

    @Test
    void actualizarDoctor_exitoso() {
        Especialidad esp = new Especialidad("Nutrición");
        ReflectionTestUtils.setField(esp, "id", 15L);
        Doctor doc = new Doctor("Dr. Antiguo", "CMP-OLD", Set.of());
        ReflectionTestUtils.setField(doc, "id", 3L);
        DoctorAdminRequest request = new DoctorAdminRequest("Dr. Actualizado", "CMP-NEW", List.of(15L));

        when(doctores.findById(3L)).thenReturn(Optional.of(doc));
        when(doctores.existsByCmpIgnoreCaseAndIdNot("CMP-NEW", 3L)).thenReturn(false);
        when(especialidades.findAllById(any())).thenReturn(List.of(esp));
        when(doctores.saveAndFlush(doc)).thenReturn(doc);

        var resp = service.actualizarDoctor(3L, request);
        assertEquals("Dr. Actualizado", resp.nombre());
        assertEquals("CMP-NEW", resp.cmp());
    }

    @Test
    void cambiarEstadoDoctor_noExisteLanza404() {
        when(doctores.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.cambiarEstadoDoctor(99L, "INACTIVO"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void cambiarEstadoDoctor_exitoso() {
        Doctor doc = new Doctor("Dr. Demo", "CMP-123", Set.of());
        ReflectionTestUtils.setField(doc, "id", 4L);
        when(doctores.findById(4L)).thenReturn(Optional.of(doc));
        when(doctores.save(doc)).thenReturn(doc);

        var resp = service.cambiarEstadoDoctor(4L, "INACTIVO");
        assertEquals("INACTIVO", resp.estado());
    }

    // === HORARIO ===

    @Test
    void listarHorarios_rangoInvalidoLanza400() {
        LocalDate start = LocalDate.of(2026, 11, 20);
        LocalDate end = LocalDate.of(2026, 11, 10);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.listarHorarios(1L, null, start, end));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void listarHorarios_dosRecursosLanza400() {
        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate end = LocalDate.of(2026, 11, 20);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.listarHorarios(1L, 2L, start, end));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void listarHorarios_filtroDoctorExitoso() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-1", Set.of());
        ReflectionTestUtils.setField(doctor, "id", 1L);
        Horario h = new Horario(doctor, LocalDate.of(2026, 11, 10), LocalTime.of(9, 0));
        ReflectionTestUtils.setField(h, "id", 100L);
        LocalDate start = LocalDate.of(2026, 11, 9);
        LocalDate end = LocalDate.of(2026, 11, 15);

        when(horarios.findByDoctor_IdAndFechaBetweenOrderByFechaAscHoraAsc(1L, start, end))
                .thenReturn(List.of(h));

        var list = service.listarHorarios(1L, null, start, end);
        assertEquals(1, list.size());
        assertEquals(100L, list.getFirst().id());
    }

    @Test
    void obtenerHorario_noExisteLanza404() {
        when(horarios.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.obtenerHorario(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void crearHorario_recursoInvalidoLanza400() {
        HorarioAdminRequest reqAmbos = new HorarioAdminRequest(1L, 2L, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        ApiException ex1 = assertThrows(ApiException.class, () -> service.crearHorario(reqAmbos));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        HorarioAdminRequest reqNinguno = new HorarioAdminRequest(null, null, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        ApiException ex2 = assertThrows(ApiException.class, () -> service.crearHorario(reqNinguno));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());
    }

    @Test
    void crearHorario_fechaPasadaLanza400() {
        HorarioAdminRequest req = new HorarioAdminRequest(1L, null, LocalDate.of(2026, 11, 7), LocalTime.of(10, 0));
        ApiException ex = assertThrows(ApiException.class, () -> service.crearHorario(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("El horario debe ser futuro", ex.getMessage());
    }

    @Test
    void crearHorario_doctorNoExisteOInactivoLanza404() {
        HorarioAdminRequest req = new HorarioAdminRequest(99L, null, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(doctores.findByIdAndEstado(99L, "ACTIVO")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.crearHorario(req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void crearHorario_duplicadoLanza409() {
        Doctor doc = new Doctor("Dr. Demo", "CMP-1", Set.of());
        ReflectionTestUtils.setField(doc, "id", 1L);
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        LocalTime hora = LocalTime.of(10, 0);
        HorarioAdminRequest req = new HorarioAdminRequest(1L, null, fecha, hora);

        when(doctores.findByIdAndEstado(1L, "ACTIVO")).thenReturn(Optional.of(doc));
        when(horarios.existsByDoctor_IdAndFechaAndHora(1L, fecha, hora)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.crearHorario(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Ya existe un horario para ese recurso y momento", ex.getMessage());
    }

    @Test
    void crearHorario_doctorExitoso() {
        Doctor doc = new Doctor("Dr. Demo", "CMP-1", Set.of());
        ReflectionTestUtils.setField(doc, "id", 1L);
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        LocalTime hora = LocalTime.of(10, 0);
        HorarioAdminRequest req = new HorarioAdminRequest(1L, null, fecha, hora);

        when(doctores.findByIdAndEstado(1L, "ACTIVO")).thenReturn(Optional.of(doc));
        when(horarios.existsByDoctor_IdAndFechaAndHora(1L, fecha, hora)).thenReturn(false);
        when(horarios.saveAndFlush(any(Horario.class))).thenAnswer(i -> {
            Horario h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 50L);
            return h;
        });

        var resp = service.crearHorario(req);
        assertEquals(50L, resp.id());
        assertEquals(1L, resp.doctorId());
        assertEquals(fecha, resp.fecha());
        assertEquals(hora, resp.hora());
    }

    @Test
    void crearHorario_servicioExitoso() {
        ServicioComplementario serv = new ServicioComplementario("Ecografía");
        ReflectionTestUtils.setField(serv, "id", 5L);
        LocalDate fecha = LocalDate.of(2026, 11, 10);
        LocalTime hora = LocalTime.of(11, 0);
        HorarioAdminRequest req = new HorarioAdminRequest(null, 5L, fecha, hora);

        when(servicios.findById(5L)).thenReturn(Optional.of(serv));
        when(horarios.existsByServicioComplementario_IdAndFechaAndHora(5L, fecha, hora)).thenReturn(false);
        when(horarios.saveAndFlush(any(Horario.class))).thenAnswer(i -> {
            Horario h = i.getArgument(0);
            ReflectionTestUtils.setField(h, "id", 51L);
            return h;
        });

        var resp = service.crearHorario(req);
        assertEquals(51L, resp.id());
        assertEquals(5L, resp.servicioId());
    }

    @Test
    void eliminarHorarioRechazaCitaActivaCon409() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-DEMO-1", Set.of());
        Horario horario = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(horario));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.eliminarHorario(50L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("No se puede eliminar el horario porque tiene una cita reservada o confirmada",
                error.getMessage());
        verify(horarios, never()).delete(any(Horario.class));
    }

    @Test
    void eliminarHorario_ocupadoLanza409() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-1", Set.of());
        Horario horario = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        horario.ocupar();
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(horario));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(false);

        ApiException error = assertThrows(ApiException.class, () -> service.eliminarHorario(50L));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("No se puede eliminar el horario mientras figure como ocupado", error.getMessage());
    }

    @Test
    void eliminarHorario_conCitasHistoricasLanza409() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-1", Set.of());
        Horario horario = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(horario));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(false);
        when(citas.existsByHorario_Id(50L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service.eliminarHorario(50L));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertEquals("No se puede eliminar el horario porque conserva citas históricas vinculadas", error.getMessage());
    }

    @Test
    void eliminarHorario_exitoso() {
        Doctor doctor = new Doctor("Dra. Demo", "CMP-1", Set.of());
        Horario horario = new Horario(doctor, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(horario));
        when(citas.existsByHorario_IdAndEstadoIn(50L, EstadoCita.ACTIVAS)).thenReturn(false);
        when(citas.existsByHorario_Id(50L)).thenReturn(false);

        service.eliminarHorario(50L);
        verify(horarios).delete(horario);
    }

    // === RECEPCIONISTA ===

    @Test
    void listarRecepcionistas_retornaLista() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.RECEPCIONISTA);
        Recepcionista rec = new Recepcionista(user, "Ana", "Gomez", "Perez", "12345678", "ana@example.test");
        when(recepcionistas.findAllByOrderByApellidoPaternoAscNombreAsc()).thenReturn(List.of(rec));

        var list = service.listarRecepcionistas();
        assertEquals(1, list.size());
        assertEquals("Ana", list.getFirst().nombre());
    }

    @Test
    void obtenerRecepcionista_noExisteLanza404() {
        when(recepcionistas.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.obtenerRecepcionista(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void crearRecepcionista_dniDuplicadoLanza409() {
        RecepcionistaCrearRequest req = new RecepcionistaCrearRequest(
                "Maria", "Lopez", null, "87654321", "maria@example.test", null, null, "ClaveTemp1234!");
        when(usuarios.existsByDni("87654321")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.crearRecepcionista(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El DNI ya está registrado", ex.getMessage());
    }

    @Test
    void crearRecepcionista_correoDuplicadoLanza409() {
        RecepcionistaCrearRequest req = new RecepcionistaCrearRequest(
                "Maria", "Lopez", null, "87654321", "maria@example.test", null, null, "ClaveTemp1234!");
        when(usuarios.existsByDni("87654321")).thenReturn(false);
        when(recepcionistas.existsByDni("87654321")).thenReturn(false);
        when(usuarios.findByIdentifier("maria@example.test")).thenReturn(List.of(new Usuario("99999999", "h", RolUsuario.PACIENTE)));

        ApiException ex = assertThrows(ApiException.class, () -> service.crearRecepcionista(req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El correo ya está asociado a otra cuenta", ex.getMessage());
    }

    @Test
    void crearRecepcionista_exitoso() {
        RecepcionistaCrearRequest req = new RecepcionistaCrearRequest(
                "Maria", "Lopez", "Diaz", "87654321", "maria@example.test", "987654321", "Av. Principal 123", "ClaveTemp1234!");
        when(usuarios.existsByDni("87654321")).thenReturn(false);
        when(recepcionistas.existsByDni("87654321")).thenReturn(false);
        when(usuarios.findByIdentifier("maria@example.test")).thenReturn(List.of());
        when(recepcionistas.existsByCorreoIgnoreCase("maria@example.test")).thenReturn(false);
        when(encoder.encode("ClaveTemp1234!")).thenReturn("hashedTemp");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(recepcionistas.saveAndFlush(any(Recepcionista.class))).thenAnswer(i -> {
            Recepcionista r = i.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 10L);
            return r;
        });

        var resp = service.crearRecepcionista(req);
        assertEquals(10L, resp.id());
        assertEquals("Maria", resp.nombre());
        assertEquals("87654321", resp.dni());
    }

    @Test
    void actualizarRecepcionista_noExisteLanza404() {
        RecepcionistaActualizarRequest req = new RecepcionistaActualizarRequest(
                "Maria", "Lopez", null, "maria@example.test", null, null);
        when(recepcionistas.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizarRecepcionista(99L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void actualizarRecepcionista_correoDuplicadoLanza409() {
        Usuario u1 = new Usuario("11112222", "h", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(u1, "id", 1L);
        Recepcionista r1 = new Recepcionista(u1, "Maria", "Lopez", null, "11112222", "maria@example.test");
        ReflectionTestUtils.setField(r1, "id", 5L);

        RecepcionistaActualizarRequest req = new RecepcionistaActualizarRequest(
                "Maria", "Lopez", null, "otro@example.test", null, null);

        when(recepcionistas.findById(5L)).thenReturn(Optional.of(r1));
        when(recepcionistas.existsByCorreoIgnoreCaseAndIdNot("otro@example.test", 5L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizarRecepcionista(5L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El correo ya está asociado a otra cuenta", ex.getMessage());
    }

    @Test
    void actualizarRecepcionista_exitoso() {
        Usuario u1 = new Usuario("11112222", "h", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(u1, "id", 1L);
        Recepcionista r1 = new Recepcionista(u1, "Maria", "Lopez", null, "11112222", "maria@example.test");
        ReflectionTestUtils.setField(r1, "id", 5L);

        RecepcionistaActualizarRequest req = new RecepcionistaActualizarRequest(
                "Maria Elena", "Lopez", "Ruiz", "nuevo@example.test", "911222333", "Calle 1");

        when(recepcionistas.findById(5L)).thenReturn(Optional.of(r1));
        when(recepcionistas.existsByCorreoIgnoreCaseAndIdNot("nuevo@example.test", 5L)).thenReturn(false);
        when(usuarios.findByIdentifier("nuevo@example.test")).thenReturn(List.of());
        when(recepcionistas.saveAndFlush(r1)).thenReturn(r1);

        var resp = service.actualizarRecepcionista(5L, req);
        assertEquals("Maria Elena", resp.nombre());
        assertEquals("nuevo@example.test", resp.correo());
    }

    @Test
    void cambiarEstadoRecepcionista_noExisteLanza404() {
        when(recepcionistas.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.cambiarEstadoRecepcionista(99L, "INACTIVO"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void cambiarEstadoRecepcionista_exitoso() {
        Usuario u1 = new Usuario("11112222", "h", RolUsuario.RECEPCIONISTA);
        Recepcionista r1 = new Recepcionista(u1, "Maria", "Lopez", null, "11112222", "maria@example.test");
        ReflectionTestUtils.setField(r1, "id", 5L);

        when(recepcionistas.findById(5L)).thenReturn(Optional.of(r1));
        when(recepcionistas.save(r1)).thenReturn(r1);

        var resp = service.cambiarEstadoRecepcionista(5L, "INACTIVO");
        assertEquals("INACTIVO", resp.estado());
    }
}
