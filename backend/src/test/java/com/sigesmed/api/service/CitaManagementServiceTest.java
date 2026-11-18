package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Pago;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoPago;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.PagoRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CitaManagementServiceTest {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Instant NOW = Instant.parse("2026-11-08T15:00:00Z");
    private final Clock clock = Clock.fixed(NOW, LIMA);

    @Mock private PacienteRepository pacientes;
    @Mock private CitaRepository citas;
    @Mock private HorarioRepository horarios;
    @Mock private PagoRepository pagos;
    @Mock private UsuarioRepository usuarios;

    private CitaManagementService service;
    private Paciente patient;
    private Especialidad pediatria;
    private Especialidad odontologia;
    private Horario oldSlot;
    private Cita appointment;

    @BeforeEach
    void setUp() {
        service = new CitaManagementService(pacientes, citas, horarios, clock, Duration.ofHours(2));
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 10L);
        patient = new Paciente(user, "Ana", "Prueba", "Demo", "12345678",
                LocalDate.of(2000, 1, 1), "ana@example.test");
        ReflectionTestUtils.setField(patient, "id", 20L);
        pediatria = specialty(30L, "Pediatría");
        odontologia = specialty(31L, "Odontología");
        oldSlot = slot(50L, pediatria, LocalDate.of(2026, 11, 9), LocalTime.of(10, 0));
        appointment = new Cita(patient, oldSlot, pediatria, EstadoCita.RESERVADA,
                OffsetDateTime.now(clock));
        ReflectionTestUtils.setField(appointment, "id", 60L);
    }

    @Test
    void cancelarRechazaCitaConMenosDeDosHoras() {
        Horario soon = slot(51L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(11, 30));
        appointment.setHorario(soon);
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));

        ApiException error = assertThrows(ApiException.class, () -> service.cancelarPropia(10L, 60L, "motivo"));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).findByIdForUpdate(anyLong());
        verify(citas, never()).save(any(Cita.class));
    }

    @Test
    void cancelarAceptaElLimiteExactoDeDosHorasYLiberaElHorario() {
        Horario exactlyTwoHours = slot(51L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(12, 0));
        appointment.setHorario(exactlyTwoHours);
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(51L)).thenReturn(Optional.of(exactlyTwoHours));
        when(citas.save(appointment)).thenReturn(appointment);

        service.cancelarPropia(10L, 60L, "cambio de planes");

        assertEquals(EstadoCita.CANCELADA, appointment.getEstado());
        assertEquals(true, exactlyTwoHours.isDisponible());
        verify(horarios).save(exactlyTwoHours);
        verify(citas).save(appointment);
    }

    @Test
    void reprogramarRechazaOtraEspecialidad() {
        Horario newSlot = slot(52L, odontologia, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reprogramarRechazaHorarioOcupado() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        newSlot.ocupar();
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void reprogramarRechazaHorarioQueNoEsFuturo() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 8), LocalTime.of(9, 59));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verify(horarios, never()).save(any(Horario.class));
        verify(citas, never()).saveAndFlush(any(Cita.class));
    }

    @Test
    void pacienteNoPuedeObtenerCitaDeOtraPersona() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.empty());

        ApiException error = assertThrows(ApiException.class,
                () -> service.reprogramarPropia(10L, 60L, 52L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(citas, never()).findByIdForUpdate(anyLong());
        verify(horarios, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void reprogramacionValidaLiberaElHorarioAnteriorYOcupaElNuevo() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findByIdAndPaciente_IdForUpdate(60L, 20L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));
        when(citas.existsActiveForPatientAt(20L, newSlot.getFecha(), newSlot.getHora(),
                Set.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA))).thenReturn(false);
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(oldSlot));
        when(citas.saveAndFlush(appointment)).thenReturn(appointment);

        service.reprogramarPropia(10L, 60L, 52L);

        assertSame(newSlot, appointment.getHorario());
        assertEquals(true, oldSlot.isDisponible());
        assertEquals(false, newSlot.isDisponible());
        verify(horarios).save(oldSlot);
        verify(horarios).save(newSlot);
        verify(citas).saveAndFlush(appointment);
    }

    // === LISTAR AGENDA ===

    @Test
    void listarAgenda_porEstadoYFechaSinBusqueda() {
        when(citas.findAgenda(EstadoCita.RESERVADA, LocalDate.of(2026, 11, 9)))
                .thenReturn(List.of(appointment));

        var result = service.listarAgenda(EstadoCita.RESERVADA, LocalDate.of(2026, 11, 9), null);
        assertEquals(1, result.size());
        assertEquals(60L, result.getFirst().id());
    }

    @Test
    void listarAgenda_conBusquedaPorNombrePaciente() {
        when(citas.findAgenda(null, null)).thenReturn(List.of(appointment));

        var result = service.listarAgenda(null, null, "Ana");
        assertEquals(1, result.size());

        var empty = service.listarAgenda(null, null, "NombreInexistente");
        assertEquals(0, empty.size());
    }

    @Test
    void listarAgenda_conBusquedaPorDniPaciente() {
        when(citas.findAgenda(null, null)).thenReturn(List.of(appointment));

        var result = service.listarAgenda(null, null, "12345678");
        assertEquals(1, result.size());
    }

    @Test
    void listarAgenda_conBusquedaPorServicioComplementario() {
        ServicioComplementario serv = new ServicioComplementario("Radiología");
        ReflectionTestUtils.setField(serv, "id", 80L);
        Horario servSlot = new Horario(serv, LocalDate.of(2026, 11, 9), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(servSlot, "id", 85L);
        Cita servCita = new Cita(patient, servSlot, serv, EstadoCita.RESERVADA, OffsetDateTime.now(clock));
        ReflectionTestUtils.setField(servCita, "id", 88L);

        when(citas.findAgenda(null, null)).thenReturn(List.of(servCita));

        var result = service.listarAgenda(null, null, "Radiología");
        assertEquals(1, result.size());
        assertEquals(88L, result.getFirst().id());
    }

    // === CAMBIAR ESTADO POR RECEPCION ===

    @Test
    void cambiarEstadoPorRecepcion_citaNoExisteLanza404() {
        when(citas.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cambiarEstadoPorRecepcion(99L, EstadoCita.ATENDIDA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void cambiarEstadoPorRecepcion_estadoInvalidoLanza400() {
        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cambiarEstadoPorRecepcion(60L, EstadoCita.CONFIRMADA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Recepción solo puede marcar una cita como atendida, inasistencia o cancelada", ex.getMessage());
    }

    @Test
    void cambiarEstadoPorRecepcion_citaInactivaLanza409() {
        appointment.setEstado(EstadoCita.CANCELADA);
        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cambiarEstadoPorRecepcion(60L, EstadoCita.ATENDIDA));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("La cita ya no está activa", ex.getMessage());
    }

    @Test
    void cambiarEstadoPorRecepcion_horarioFuturoLanza409() {
        // appointment is at 2026-11-09 10:00 (future relative to clock 2026-11-08 10:00)
        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cambiarEstadoPorRecepcion(60L, EstadoCita.ATENDIDA));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("No se puede cerrar una cita antes de su horario", ex.getMessage());
    }

    @Test
    void cambiarEstadoPorRecepcion_atendidaConHorarioPasadoExitoso() {
        Horario pastSlot = slot(70L, pediatria, LocalDate.of(2026, 11, 7), LocalTime.of(10, 0));
        appointment.setHorario(pastSlot);

        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));
        when(citas.save(appointment)).thenReturn(appointment);

        var resp = service.cambiarEstadoPorRecepcion(60L, EstadoCita.ATENDIDA);
        assertEquals(EstadoCita.ATENDIDA, appointment.getEstado());
        assertEquals(EstadoCita.ATENDIDA, resp.estado());
        verify(citas).save(appointment);
    }

    @Test
    void cambiarEstadoPorRecepcion_noAsistioConHorarioPasadoExitoso() {
        Horario pastSlot = slot(70L, pediatria, LocalDate.of(2026, 11, 7), LocalTime.of(10, 0));
        appointment.setHorario(pastSlot);

        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));
        when(citas.save(appointment)).thenReturn(appointment);

        var resp = service.cambiarEstadoPorRecepcion(60L, EstadoCita.NO_ASISTIO);
        assertEquals(EstadoCita.NO_ASISTIO, appointment.getEstado());
        assertEquals(EstadoCita.NO_ASISTIO, resp.estado());
        verify(citas).save(appointment);
    }

    // === CANCELAR Y REPROGRAMAR POR RECEPCION ===

    @Test
    void cancelarPorRecepcion_exitoso() {
        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(oldSlot));
        when(citas.save(appointment)).thenReturn(appointment);

        var resp = service.cancelarPorRecepcion(60L, "Cancelación en ventanilla");
        assertEquals(EstadoCita.CANCELADA, resp.estado());
        assertEquals(true, oldSlot.isDisponible());
        verify(horarios).save(oldSlot);
        verify(citas).save(appointment);
    }

    @Test
    void reprogramarPorRecepcion_exitoso() {
        Horario newSlot = slot(52L, pediatria, LocalDate.of(2026, 11, 10), LocalTime.of(10, 0));
        when(citas.findByIdForUpdate(60L)).thenReturn(Optional.of(appointment));
        when(horarios.findByIdForUpdate(52L)).thenReturn(Optional.of(newSlot));
        when(horarios.findByIdForUpdate(50L)).thenReturn(Optional.of(oldSlot));
        when(citas.existsActiveForPatientAt(20L, newSlot.getFecha(), newSlot.getHora(),
                Set.of(EstadoCita.RESERVADA, EstadoCita.CONFIRMADA))).thenReturn(false);
        when(citas.saveAndFlush(appointment)).thenReturn(appointment);

        var resp = service.reprogramarPorRecepcion(60L, 52L);
        assertSame(newSlot, appointment.getHorario());
        assertEquals(true, oldSlot.isDisponible());
        assertEquals(false, newSlot.isDisponible());
        verify(horarios).save(oldSlot);
        verify(horarios).save(newSlot);
    }

    // === OBTENER Y LISTAR PROPIAS ===

    @Test
    void obtenerParaRecepcion_noExisteLanza404() {
        when(citas.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.obtenerParaRecepcion(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void obtenerParaRecepcion_exitoso() {
        when(citas.findById(60L)).thenReturn(Optional.of(appointment));
        var resp = service.obtenerParaRecepcion(60L);
        assertEquals(60L, resp.id());
    }

    @Test
    void listarPropias_pacienteNoExisteLanza404() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> service.listarPropias(10L, "PROXIMAS"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void listarPropias_vistaInvalidaLanza400() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        ApiException ex = assertThrows(ApiException.class, () -> service.listarPropias(10L, "OTRA"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void listarPropias_vistaProximasExitoso() {
        when(pacientes.findByUsuario_Id(10L)).thenReturn(Optional.of(patient));
        when(citas.findPatientCitas(eq(20L), anyCollection())).thenReturn(List.of(appointment));

        var list = service.listarPropias(10L, "PROXIMAS");
        assertEquals(1, list.size());
    }

    @Test
    void registrarPagoPresencial_efectivoYTarjetaConfirmaCitaYGeneraComprobante() {
        service = new CitaManagementService(pacientes, citas, horarios, pagos, usuarios, clock, Duration.ofHours(2));
        Usuario recep = new Usuario("48291637", "h", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(recep, "id", 99L);
        when(citas.findById(30L)).thenReturn(Optional.of(appointment));
        when(usuarios.findById(99L)).thenReturn(Optional.of(recep));
        when(pagos.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 501L);
            return p;
        });

        var receipt = service.registrarPagoPresencial(30L, 99L, "efectivo", new BigDecimal("80.00"));
        assertEquals(EstadoCita.CONFIRMADA, appointment.getEstado());
        assertEquals(EstadoPago.PAGADO, receipt.estadoPago());
        assertEquals("EFECTIVO", receipt.metodo());
        assertEquals("CMP-PAGO-000501", receipt.codigoComprobante());
        assertEquals("48291637", receipt.registradoPorDni());
        assertSame(recep, appointment.getPago().getRegistradoPor());
    }

    @Test
    void registrarPagoPresencial_rechazaDoblePagoYValoresInvalidos() {
        service = new CitaManagementService(pacientes, citas, horarios, pagos, usuarios, clock, Duration.ofHours(2));
        Pago yaPagado = new Pago("TARJETA", new BigDecimal("80.00"), EstadoPago.PAGADO, new Usuario("48291637", "h", RolUsuario.RECEPCIONISTA));
        ReflectionTestUtils.setField(appointment, "pago", yaPagado);
        when(citas.findById(30L)).thenReturn(Optional.of(appointment));

        ApiException dup = assertThrows(ApiException.class,
                () -> service.registrarPagoPresencial(30L, 99L, "EFECTIVO", new BigDecimal("80.00")));
        assertEquals(HttpStatus.CONFLICT, dup.getStatus());

        ApiException neg = assertThrows(ApiException.class,
                () -> service.registrarPagoPresencial(30L, 99L, "EFECTIVO", new BigDecimal("-10.00")));
        assertEquals(HttpStatus.BAD_REQUEST, neg.getStatus());

        ApiException noMethod = assertThrows(ApiException.class,
                () -> service.registrarPagoPresencial(30L, 99L, " ", new BigDecimal("80.00")));
        assertEquals(HttpStatus.BAD_REQUEST, noMethod.getStatus());
    }

    private Especialidad specialty(Long id, String name) {
        Especialidad value = new Especialidad(name);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }

    private Horario slot(Long id, Especialidad specialty, LocalDate date, LocalTime time) {
        Doctor doctor = new Doctor("Dr. Demo", "CMP-DEMO-" + id, Set.of(specialty));
        ReflectionTestUtils.setField(doctor, "id", id + 100);
        Horario value = new Horario(doctor, date, time);
        ReflectionTestUtils.setField(value, "id", id);
        return value;
    }
}
