package com.sigesmed.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.controller.CitaController;
import com.sigesmed.api.service.CitaManagementService;
import com.sigesmed.api.service.CitaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CitaController.class)
@Import(SecurityConfiguration.class)
class RoleAuthorizationTest {
    @Autowired private MockMvc mvc;
    @MockBean private CitaService citaService;
    @MockBean private CitaManagementService citaManagementService;

    @Test
    @WithMockUser(roles = "PACIENTE")
    void pacienteNoPuedeUsarRutaDeAdministrador() throws Exception {
        mvc.perform(get("/api/v1/administrador/doctores")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorNoPuedeUsarRutaDeRecepcionista() throws Exception {
        mvc.perform(get("/api/v1/recepcionista/citas")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RECEPCIONISTA")
    void recepcionistaNoPuedeUsarRutaDePaciente() throws Exception {
        mvc.perform(get("/api/v1/citas/mias")).andExpect(status().isForbidden());
    }
}
