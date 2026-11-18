package com.sigesmed.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.dto.disponibilidad.DoctorResponse;
import com.sigesmed.api.dto.disponibilidad.EspecialidadResponse;
import com.sigesmed.api.dto.disponibilidad.HorarioResponse;
import com.sigesmed.api.dto.disponibilidad.ServicioComplementarioResponse;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaManagementService;
import com.sigesmed.api.service.CitaService;
import com.sigesmed.api.service.DisponibilidadService;

@WebMvcTest({ CitaController.class, DisponibilidadController.class, HealthController.class })
@Import(SecurityConfiguration.class)
class CitaAndDisponibilidadControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private CitaService citaService;
    @MockitoBean private CitaManagementService managementService;
    @MockitoBean private DisponibilidadService disponibilidadService;

    private UsuarioPrincipal pacientePrincipal() {
        Usuario u = new Usuario("70000001", "h", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(u, "id", 10L);
        return new UsuarioPrincipal(u);
    }

    @Test
    void disponibilidadYHealthEndpoints() throws Exception {
        when(disponibilidadService.especialidades()).thenReturn(List.of(new EspecialidadResponse(1L, "Pediatría")));
        when(disponibilidadService.serviciosComplementarios()).thenReturn(List.of(new ServicioComplementarioResponse(1L, "Laboratorio")));
        when(disponibilidadService.medicos(1L)).thenReturn(List.of(new DoctorResponse(1L, "Dr. Ana", "DEV-CMP-0001", List.of("Pediatría"))));
        when(disponibilidadService.horarios(eq(1L), any(), any())).thenReturn(List.of(new HorarioResponse(5L, 1L, "Dr. Ana", null, null, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0))));
        when(disponibilidadService.horariosServicio(eq(1L), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/especialidades")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/servicios-complementarios")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/especialidades/1/medicos")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/medicos/1/horarios").param("fecha", "2026-11-20")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/servicios-complementarios/1/horarios").param("fecha", "2026-11-20")
                .with(user(pacientePrincipal()))).andExpect(status().isOk());
    }

    @Test
    void citasPacienteEndpoints() throws Exception {
        UsuarioPrincipal p = pacientePrincipal();
        CitaResponse c = new CitaResponse(15L, 5L, 1L, EstadoCita.RESERVADA, "Ana", "70000001", "a@test.pe",
                List.of("Pediatría"), "Dr. Ana", LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), null, null, null);
        when(citaService.reservar(10L, 5L, 1L, null)).thenReturn(c);
        when(citaService.obtenerPropia(10L, 15L)).thenReturn(c);
        when(managementService.listarPropias(10L, "PROXIMAS")).thenReturn(List.of(c));
        when(managementService.cancelarPropia(10L, 15L, "Motivo")).thenReturn(c);
        when(managementService.reprogramarPropia(10L, 15L, 6L)).thenReturn(c);

        mvc.perform(post("/api/v1/citas").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horarioId\":5,\"especialidadId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15));
        mvc.perform(get("/api/v1/citas/15").with(user(p))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/citas/mias").with(user(p))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/citas/15/cancelacion").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/citas/15/horario").with(user(p)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horarioId\":6}"))
                .andExpect(status().isOk());
    }
}
