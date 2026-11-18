package com.sigesmed.api.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.repository.AdministradorRepository;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;
import com.sigesmed.api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevDataInitializerTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private RecepcionistaRepository recepcionistas;
    @Mock private AdministradorRepository administradores;
    @Mock private EspecialidadRepository especialidades;
    @Mock private DoctorRepository doctores;
    @Mock private CitaRepository citas;
    @Mock private HorarioRepository horarios;
    @Mock private ServicioComplementarioRepository servicios;
    @Mock private PasswordEncoder encoder;

    @Test
    void run_siembraEspecialidadesDoctoresHorariosYCuentasDemo() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneId.of("America/Lima"));
        DevDataInitializer init = new DevDataInitializer(
                usuarios, recepcionistas, administradores, especialidades, doctores, citas, horarios, servicios,
                encoder, clock, "AdminPass#123", "RecepPass#123");

        when(encoder.encode(anyString())).thenReturn("$2a$10$hash");
        Usuario admin = new Usuario("74829105", "$2a$10$hash", RolUsuario.ADMINISTRADOR);
        Usuario recep = new Usuario("48291637", "$2a$10$hash", RolUsuario.RECEPCIONISTA);
        when(usuarios.findByDni("74829105")).thenReturn(Optional.of(admin));
        when(usuarios.findByDni("48291637")).thenReturn(Optional.of(recep));
        when(usuarios.findByDni("62948173")).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(administradores.findByUsuario_Id(any())).thenReturn(Optional.empty());
        when(recepcionistas.findByUsuario_Id(any())).thenReturn(Optional.empty());
        when(especialidades.findByNombreIgnoreCase(anyString())).thenAnswer(i -> Optional.of(new Especialidad(i.getArgument(0))));
        when(especialidades.findAll()).thenReturn(List.of(new Especialidad("Pediatría general")));
        when(servicios.findAllByOrderByNombreAsc()).thenReturn(List.of(new ServicioComplementario("Laboratorio")));
        when(doctores.findByCmpIgnoreCase(anyString())).thenReturn(Optional.empty());
        Doctor doc = new Doctor("Dra. Carmen", "DEV-CMP-0001", Set.of(new Especialidad("Pediatría general")));
        when(doctores.save(any(Doctor.class))).thenReturn(doc);
        when(doctores.findAll()).thenReturn(List.of(doc));
        when(horarios.findAllByFechaBetweenOrderByFechaAscHoraAsc(any(), any())).thenReturn(List.of());

        init.run(new DefaultApplicationArguments());

        verify(doctores, atLeastOnce()).save(any(Doctor.class));
        verify(horarios, atLeastOnce()).saveAll(any());
    }
}
