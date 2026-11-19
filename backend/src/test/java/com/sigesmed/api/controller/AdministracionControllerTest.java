package com.sigesmed.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.dto.administracion.DoctorAdminResponse;
import com.sigesmed.api.dto.administracion.HorarioAdminResponse;
import com.sigesmed.api.dto.administracion.HorarioRecurrenteResponse;
import com.sigesmed.api.dto.administracion.RecepcionistaAdminResponse;
import com.sigesmed.api.service.AdministracionService;

@WebMvcTest(AdministracionController.class)
@Import(SecurityConfiguration.class)
@WithMockUser(roles = "ADMINISTRADOR")
class AdministracionControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AdministracionService service;

    @Test
    void doctoresCrudYEstado() throws Exception {
        DoctorAdminResponse doc = new DoctorAdminResponse(1L, "Dr. Ana", "DEV-CMP-0001", "ACTIVO", List.of(1L), List.of("Pediatría"));
        when(service.listarDoctores()).thenReturn(List.of(doc));
        when(service.obtenerDoctor(1L)).thenReturn(doc);
        when(service.crearDoctor(any())).thenReturn(doc);
        when(service.actualizarDoctor(eq(1L), any())).thenReturn(doc);
        when(service.cambiarEstadoDoctor(1L, "INACTIVO"))
                .thenReturn(new DoctorAdminResponse(1L, "Dr. Ana", "DEV-CMP-0001", "INACTIVO", List.of(1L), List.of("Pediatría")));

        mvc.perform(get("/api/v1/administrador/doctores")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/v1/administrador/doctores/1")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/administrador/doctores").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dr. Ana\",\"cmp\":\"DEV-CMP-0001\",\"especialidadIds\":[1]}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/administrador/doctores/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Dr. Ana\",\"cmp\":\"DEV-CMP-0001\",\"especialidadIds\":[1]}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/administrador/doctores/1/estado").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void horariosCrudYDelete() throws Exception {
        HorarioAdminResponse h = new HorarioAdminResponse(10L, 1L, "Dr. Ana", null, null, LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), true, EstadoHorario.DISPONIBLE);
        when(service.listarHorarios(any(), any(), any(), any())).thenReturn(List.of(h));
        when(service.obtenerHorario(10L)).thenReturn(h);
        when(service.crearHorario(any())).thenReturn(h);
        when(service.actualizarHorario(eq(10L), any())).thenReturn(h);
        doNothing().when(service).eliminarHorario(10L);

        mvc.perform(get("/api/v1/administrador/horarios")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/administrador/horarios/10")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/administrador/horarios").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"fecha\":\"2026-11-20\",\"hora\":\"09:00\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/administrador/horarios/10").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"fecha\":\"2026-11-20\",\"hora\":\"09:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/administrador/horarios/10").with(csrf())).andExpect(status().isNoContent());

        HorarioRecurrenteResponse recResp = new HorarioRecurrenteResponse(1L, "Dr. Ana", 8, List.of(h));
        when(service.crearHorariosRecurrentes(any())).thenReturn(recResp);
        mvc.perform(post("/api/v1/administrador/horarios/recurrentes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"diasSemana\":[\"MONDAY\",\"WEDNESDAY\"],\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\",\"fechaDesde\":\"2026-11-23\",\"fechaHasta\":\"2026-11-27\",\"diasDescanso\":[\"2026-11-27\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creados").value(8));
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void horariosRecurrentes_denegadoAOtrosRoles() throws Exception {
        mvc.perform(post("/api/v1/administrador/horarios/recurrentes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"diasSemana\":[\"MONDAY\"],\"horaInicio\":\"08:00\",\"horaFin\":\"12:00\",\"fechaDesde\":\"2026-11-23\",\"fechaHasta\":\"2026-11-27\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionistasCrudYEstado() throws Exception {
        RecepcionistaAdminResponse rec = new RecepcionistaAdminResponse(2L, "Rosa", "Mendoza", "Diaz", "48291637", "r@test.pe", "987654321", "Lima", "ACTIVO");
        when(service.listarRecepcionistas()).thenReturn(List.of(rec));
        when(service.obtenerRecepcionista(2L)).thenReturn(rec);
        when(service.crearRecepcionista(any())).thenReturn(rec);
        when(service.actualizarRecepcionista(eq(2L), any())).thenReturn(rec);
        when(service.cambiarEstadoRecepcionista(2L, "INACTIVO")).thenReturn(rec);

        mvc.perform(get("/api/v1/administrador/recepcionistas")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/administrador/recepcionistas/2")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/administrador/recepcionistas").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rosa\",\"apellidoPaterno\":\"Mendoza\",\"dni\":\"48291637\",\"correo\":\"r@test.pe\",\"claveTemporal\":\"ClaveTemporal#12\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/administrador/recepcionistas/2").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Rosa\",\"apellidoPaterno\":\"Mendoza\",\"correo\":\"r@test.pe\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/administrador/recepcionistas/2/estado").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk());
    }
}
