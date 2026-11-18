package com.sigesmed.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoPago;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.cita.CitaResponse;
import com.sigesmed.api.dto.cita.ComprobantePagoResponse;
import com.sigesmed.api.dto.cuenta.PerfilResponse;
import com.sigesmed.api.security.UsuarioPrincipal;
import com.sigesmed.api.service.CitaManagementService;
import com.sigesmed.api.service.CuentaService;

@WebMvcTest({ RecepcionistaController.class, CuentaController.class })
@Import(SecurityConfiguration.class)
class RecepcionistaAndCuentaControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private CitaManagementService managementService;
    @MockitoBean private CuentaService cuentaService;

    private CitaResponse sampleCita() {
        return new CitaResponse(10L, 5L, 1L, EstadoCita.RESERVADA, "Ana", "70000001", "a@test.pe",
                List.of("Pediatría"), "Dr. Ana", LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), null, null, null);
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaEndpoints() throws Exception {
        CitaResponse c = sampleCita();
        when(managementService.listarAgenda(any(), any(), any())).thenReturn(List.of(c));
        when(managementService.obtenerParaRecepcion(10L)).thenReturn(c);
        when(managementService.cancelarPorRecepcion(10L, "Motivo")).thenReturn(c);
        when(managementService.reprogramarPorRecepcion(10L, 6L)).thenReturn(c);
        when(managementService.cambiarEstadoPorRecepcion(10L, EstadoCita.ATENDIDA)).thenReturn(c);

        mvc.perform(get("/api/v1/recepcionista/citas")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(10));
        mvc.perform(get("/api/v1/recepcionista/citas/10")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/recepcionista/citas/10/cancelacion").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/recepcionista/citas/10/horario").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"horarioId\":6}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/recepcionista/citas/10/estado").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ATENDIDA\"}"))
                .andExpect(status().isOk());

        Usuario recep = new Usuario("48291637", "h", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(recep, "id", 88L);
        ComprobantePagoResponse comp = new ComprobantePagoResponse(12L, 10L, "CMP-PAGO-000012", "EFECTIVO",
                new java.math.BigDecimal("80.00"), EstadoPago.PAGADO, EstadoCita.CONFIRMADA, "Ana", "70000001",
                "Pediatría", "Dr. Ana", LocalDate.of(2026, 11, 20), LocalTime.of(9, 0), "48291637");
        when(managementService.registrarPagoPresencial(10L, 88L, "EFECTIVO", new java.math.BigDecimal("80.00")))
                .thenReturn(comp);

        mvc.perform(post("/api/v1/recepcionista/citas/10/pago")
                        .with(user(new UsuarioPrincipal(recep)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"EFECTIVO\",\"monto\":80.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoComprobante").value("CMP-PAGO-000012"))
                .andExpect(jsonPath("$.estadoCita").value("CONFIRMADA"));

        mvc.perform(post("/api/v1/recepcionista/citas/10/pago")
                        .with(user(new UsuarioPrincipal(recep)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metodo\":\"\",\"monto\":-5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cuentaEndpoints() throws Exception {
        Usuario u = new Usuario("70000001", "h", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(u, "id", 7L);
        UsuarioPrincipal principal = new UsuarioPrincipal(u);
        PerfilResponse perfil = new PerfilResponse(RolUsuario.PACIENTE, "70000001", "Ana", "Torres", null, "a@test.pe", "987654321", "Lima");
        when(cuentaService.obtener(7L)).thenReturn(perfil);
        when(cuentaService.actualizar(eq(7L), any())).thenReturn(perfil);

        mvc.perform(get("/api/v1/cuenta/me").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("70000001"));
        mvc.perform(put("/api/v1/cuenta/me").with(user(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"a@test.pe\",\"celular\":\"987654321\",\"direccion\":\"Lima\"}"))
                .andExpect(status().isOk());
    }
}
