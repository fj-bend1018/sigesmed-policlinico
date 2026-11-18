package com.sigesmed.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaManagementService;

@WebMvcTest(DoctorPortalController.class)
@Import(SecurityConfiguration.class)
class DoctorPortalControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private DoctorRepository doctorRepository;
    @MockitoBean private CitaRepository citaRepository;
    @MockitoBean private HorarioRepository horarioRepository;
    @MockitoBean private CitaManagementService citaManagementService;

    private UsuarioPrincipal doctorPrincipal(Long userId) {
        Usuario u = new Usuario("62948173", "hash", RolUsuario.DOCTOR);
        ReflectionTestUtils.setField(u, "id", userId);
        return new UsuarioPrincipal(u);
    }

    private Doctor buildDoctor(Long id, String nombre, String cmp) {
        Especialidad esp = new Especialidad("Pediatría general");
        ReflectionTestUtils.setField(esp, "id", 1L);
        Doctor doc = new Doctor(nombre, cmp, Set.of(esp));
        ReflectionTestUtils.setField(doc, "id", id);
        doc.actualizarCorreo("doc@sigesmed.test");
        return doc;
    }

    private Cita buildCita(Long citaId, Doctor doc) {
        Usuario pu = new Usuario("70000001", "h", RolUsuario.PACIENTE);
        Paciente p = new Paciente(pu, "Ana", "Torres", "Vega", "70000001", LocalDate.of(1995, 1, 1), "ana@test.pe");
        Horario h = new Horario(doc, LocalDate.of(2026, 11, 18), LocalTime.of(9, 0));
        ReflectionTestUtils.setField(h, "id", 10L);
        Cita c = new Cita(p, h, doc.getEspecialidades().iterator().next(), EstadoCita.RESERVADA, OffsetDateTime.now());
        ReflectionTestUtils.setField(c, "id", citaId);
        return c;
    }

    @Test
    void resumen_devuelve200ParaDoctorVinculado() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen Salazar", "DEV-CMP-0001");
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));

        mvc.perform(get("/api/v1/doctor/resumen").with(user(doctorPrincipal(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Dra. Carmen Salazar"))
                .andExpect(jsonPath("$.cmp").value("DEV-CMP-0001"))
                .andExpect(jsonPath("$.especialidades[0]").value("Pediatría general"));
    }

    @Test
    void resumen_devuelve404SiPerfilNoVinculado() throws Exception {
        when(doctorRepository.findByUsuario_Id(101L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/doctor/resumen").with(user(doctorPrincipal(101L))))
                .andExpect(status().isNotFound());
    }

    @Test
    void citas_devuelve200ConListaPropia() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        Cita c = buildCita(20L, doc);
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(citaRepository.findByDoctor(eq(5L), any(), any())).thenReturn(List.of(c));

        mvc.perform(get("/api/v1/doctor/citas")
                        .param("estado", "RESERVADA")
                        .param("fecha", "2026-11-18")
                        .with(user(doctorPrincipal(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));
    }

    @Test
    void horarios_devuelve200Proximos30Dias() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        Horario h = new Horario(doc, LocalDate.of(2026, 11, 20), LocalTime.of(10, 0));
        ReflectionTestUtils.setField(h, "id", 30L);
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(horarioRepository.findByDoctor_IdAndFechaBetweenOrderByFechaAscHoraAsc(eq(5L), any(), any()))
                .thenReturn(List.of(h));

        mvc.perform(get("/api/v1/doctor/horarios").with(user(doctorPrincipal(100L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(30));
    }

    @Test
    void cambiarEstado_atendidaDevuelve200() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        Cita c = buildCita(20L, doc);
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(citaRepository.findById(20L)).thenReturn(Optional.of(c));
        CitaResponse resp = new CitaResponse(20L, 10L, 5L, EstadoCita.ATENDIDA, "Ana Torres", "70000001",
                "ana@test.pe", List.of("Pediatría general"), "Dra. Carmen", LocalDate.of(2026, 11, 18),
                LocalTime.of(9, 0), null, null, null);
        when(citaManagementService.cambiarEstadoPorRecepcion(20L, EstadoCita.ATENDIDA)).thenReturn(resp);

        mvc.perform(patch("/api/v1/doctor/citas/20/estado")
                        .with(user(doctorPrincipal(100L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ATENDIDA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));
    }

    @Test
    void cambiarEstado_noPermitidoParaEstadoReservadaDevuelve400() throws Exception {
        mvc.perform(patch("/api/v1/doctor/citas/20/estado")
                        .with(user(doctorPrincipal(100L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RESERVADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstado_citaDeOtroMedicoDevuelve403() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        Doctor otro = buildDoctor(99L, "Dr. Otro", "DEV-CMP-0099");
        Cita c = buildCita(21L, otro);
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(citaRepository.findById(21L)).thenReturn(Optional.of(c));

        mvc.perform(patch("/api/v1/doctor/citas/21/estado")
                        .with(user(doctorPrincipal(100L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"NO_ASISTIO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstado_citaInexistenteDevuelve404() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(citaRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(patch("/api/v1/doctor/citas/999/estado")
                        .with(user(doctorPrincipal(100L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ATENDIDA\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado_conflictoDevuelve409() throws Exception {
        Doctor doc = buildDoctor(5L, "Dra. Carmen", "DEV-CMP-0001");
        Cita c = buildCita(22L, doc);
        when(doctorRepository.findByUsuario_Id(100L)).thenReturn(Optional.of(doc));
        when(citaRepository.findById(22L)).thenReturn(Optional.of(c));
        when(citaManagementService.cambiarEstadoPorRecepcion(22L, EstadoCita.ATENDIDA))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "La cita ya no está activa"));

        mvc.perform(patch("/api/v1/doctor/citas/22/estado")
                        .with(user(doctorPrincipal(100L)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ATENDIDA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void endpointsDoctor_sinSesionDevuelve401() throws Exception {
        mvc.perform(get("/api/v1/doctor/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PACIENTE")
    void endpointsDoctor_pacienteDevuelve403() throws Exception {
        mvc.perform(get("/api/v1/doctor/resumen")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void endpointsDoctor_recepcionistaDevuelve403() throws Exception {
        mvc.perform(get("/api/v1/doctor/citas")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void endpointsDoctor_adminDevuelve403() throws Exception {
        mvc.perform(get("/api/v1/doctor/horarios")).andExpect(status().isForbidden());
    }
}
