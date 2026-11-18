package com.sigesmed.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.sigesmed.api.config.SecurityConfiguration;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.service.auth.AuthService;

@WebMvcTest(AuthController.class)
@Import(SecurityConfiguration.class)
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private SecurityContextRepository securityContextRepository;
    @MockitoBean private PacienteRepository pacienteRepository;
    @MockitoBean private RecepcionistaRepository recepcionistaRepository;
    @MockitoBean private DoctorRepository doctorRepository;

    @Test
    void csrf_devuelve204() throws Exception {
        mvc.perform(get("/api/v1/csrf")).andExpect(status().isNoContent());
    }

    @Test
    void registrar_devuelve201() throws Exception {
        Usuario u = new Usuario("70000001", "hash", RolUsuario.PACIENTE);
        Paciente p = new Paciente(u, "Ana", "Torres", "Vega", "70000001", LocalDate.of(1995, 5, 20), "ana@test.pe");
        when(authService.registrarPaciente(any())).thenReturn(p);

        mvc.perform(post("/api/v1/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellidoPaterno\":\"Torres\",\"apellidoMaterno\":\"Vega\",\"dni\":\"70000001\",\"fechaNacimiento\":\"1995-05-20\",\"correo\":\"ana@test.pe\",\"clave\":\"ClaveSegura#12\",\"celular\":\"987654321\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Registro completado para el DNI 70000001"));
    }

    @Test
    void registrar_datosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/auth/registro")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"dni\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_pacienteDevuelve200YPortal() throws Exception {
        Usuario u = new Usuario("70000001", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(u, "id", 1L);
        Paciente p = new Paciente(u, "Ana", "Torres", null, "70000001", LocalDate.of(1995, 5, 20), "ana@test.pe");
        when(authService.autenticar("70000001", "Clave123")).thenReturn(u);
        when(pacienteRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(p));

        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"70000001\",\"clave\":\"Clave123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PACIENTE"))
                .andExpect(jsonPath("$.portal").value("paciente"));
    }

    @Test
    void login_doctorYRecepcionistaYAdminDevuelvenSuPortal() throws Exception {
        Usuario uDoc = new Usuario("62948173", "h", RolUsuario.DOCTOR);
        ReflectionTestUtils.setField(uDoc, "id", 2L);
        Doctor d = new Doctor("Dra. Carmen", "DEV-CMP-0001", Set.of(new Especialidad("Pediatría")));
        when(authService.autenticar("62948173", "doc123")).thenReturn(uDoc);
        when(doctorRepository.findByUsuario_Id(2L)).thenReturn(Optional.of(d));

        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"62948173\",\"clave\":\"doc123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portal").value("doctor"))
                .andExpect(jsonPath("$.nombre").value("Dra. Carmen"));

        Usuario uRec = new Usuario("48291637", "h", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(uRec, "id", 3L);
        Recepcionista r = new Recepcionista(uRec, "Rosa", "Mendoza", null, "48291637", "r@test.pe");
        when(authService.autenticar("48291637", "rec123")).thenReturn(uRec);
        when(recepcionistaRepository.findByUsuario_Id(3L)).thenReturn(Optional.of(r));

        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"48291637\",\"clave\":\"rec123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portal").value("recepcionista"));

        Usuario uAdm = new Usuario("74829105", "h", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(uAdm, "id", 4L);
        when(authService.autenticar("74829105", "adm123")).thenReturn(uAdm);

        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"74829105\",\"clave\":\"adm123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portal").value("administrador"));
    }

    @Test
    void login_credencialesInvalidasDevuelve401() throws Exception {
        when(authService.autenticar("70000001", "mal"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identificador\":\"70000001\",\"clave\":\"mal\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_devuelve204() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                        .with(user("70000001").roles("PACIENTE"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void recuperarYRestablecer_devuelven200() throws Exception {
        doNothing().when(authService).solicitarRecuperacion("ana@test.pe");
        doNothing().when(authService).restablecerClave("tok123", "NuevaClave#12");

        mvc.perform(post("/api/v1/auth/recuperar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@test.pe\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/restablecer").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok123\",\"nuevaClave\":\"NuevaClave#12\"}"))
                .andExpect(status().isOk());
    }
}
