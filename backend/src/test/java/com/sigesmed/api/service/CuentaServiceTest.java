package com.sigesmed.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.sigesmed.api.domain.entity.Administrador;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.cuenta.ActualizarPerfilRequest;
import com.sigesmed.api.dto.cuenta.PerfilResponse;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.AdministradorRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private PacienteRepository pacientes;
    @Mock private RecepcionistaRepository recepcionistas;
    @Mock private AdministradorRepository administradores;
    @Mock private DoctorRepository doctores;

    private CuentaService service;

    @BeforeEach
    void setUp() {
        service = new CuentaService(usuarios, pacientes, recepcionistas, administradores, doctores);
    }

    @Test
    void obtener_cuentaNoExisteLanza404() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.obtener(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Cuenta no encontrada", ex.getMessage());
    }

    @Test
    void obtener_perfilNoExisteLanza404() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.obtener(1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Perfil no encontrado", ex.getMessage());
    }

    @Test
    void obtener_perfilPacienteExitoso() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        Paciente paciente = new Paciente(user, "Carlos", "Perez", "Gomez", "12345678",
                LocalDate.of(1990, 5, 10), "carlos@example.test");
        paciente.setCelular("987654321");
        paciente.setDireccion("Av. Lima 123");
        ReflectionTestUtils.setField(user, "paciente", paciente);
        when(usuarios.findById(1L)).thenReturn(Optional.of(user));

        PerfilResponse resp = service.obtener(1L);

        assertEquals(RolUsuario.PACIENTE, resp.rol());
        assertEquals("12345678", resp.dni());
        assertEquals("Carlos", resp.nombre());
        assertEquals("Perez", resp.apellidoPaterno());
        assertEquals("Gomez", resp.apellidoMaterno());
        assertEquals("carlos@example.test", resp.correo());
        assertEquals("987654321", resp.celular());
        assertEquals("Av. Lima 123", resp.direccion());
    }

    @Test
    void obtener_perfilRecepcionistaExitoso() {
        Usuario user = new Usuario("87654321", "hash", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(user, "id", 2L);
        Recepcionista rec = new Recepcionista(user, "Laura", "Mendoza", null, "87654321", "laura@example.test");
        rec.actualizar("Laura", "Mendoza", null, "laura@example.test", "911222333", "Calle Real 456");
        ReflectionTestUtils.setField(user, "recepcionista", rec);
        when(usuarios.findById(2L)).thenReturn(Optional.of(user));

        PerfilResponse resp = service.obtener(2L);

        assertEquals(RolUsuario.RECEPCIONISTA, resp.rol());
        assertEquals("87654321", resp.dni());
        assertEquals("Laura", resp.nombre());
        assertEquals("Mendoza", resp.apellidoPaterno());
        assertNull(resp.apellidoMaterno());
        assertEquals("laura@example.test", resp.correo());
        assertEquals("911222333", resp.celular());
        assertEquals("Calle Real 456", resp.direccion());
    }

    @Test
    void obtener_perfilAdministradorExitoso() {
        Usuario user = new Usuario("11223344", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(user, "id", 3L);
        Administrador admin = new Administrador(user, "11223344", "admin@example.test");
        admin.actualizar("admin@example.test", "999888777", "Sede Central");
        ReflectionTestUtils.setField(user, "administrador", admin);
        when(usuarios.findById(3L)).thenReturn(Optional.of(user));

        PerfilResponse resp = service.obtener(3L);

        assertEquals(RolUsuario.ADMINISTRADOR, resp.rol());
        assertEquals("11223344", resp.dni());
        assertNull(resp.nombre());
        assertNull(resp.apellidoPaterno());
        assertNull(resp.apellidoMaterno());
        assertEquals("admin@example.test", resp.correo());
        assertEquals("999888777", resp.celular());
        assertEquals("Sede Central", resp.direccion());
    }

    @Test
    void actualizar_cuentaNoExisteLanza404() {
        ActualizarPerfilRequest req = new ActualizarPerfilRequest("nuevo@example.test", "987654321", "Dir");
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizar(99L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void actualizar_correoAsociadoAOtraCuentaLanza409() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        Usuario other = new Usuario("87654321", "hash2", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(other, "id", 2L);

        ActualizarPerfilRequest req = new ActualizarPerfilRequest("otro@example.test", "987654321", "Dir");
        when(usuarios.findById(1L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("otro@example.test")).thenReturn(List.of(other));

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizar(1L, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El correo ya está asociado a otra cuenta", ex.getMessage());
    }

    @Test
    void actualizar_perfilNoExisteLanza404() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        ActualizarPerfilRequest req = new ActualizarPerfilRequest("nuevo@example.test", "987654321", "Dir");

        when(usuarios.findById(1L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("nuevo@example.test")).thenReturn(List.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.actualizar(1L, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void actualizar_perfilPacienteExitoso() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        Paciente paciente = new Paciente(user, "Carlos", "Perez", "Gomez", "12345678",
                LocalDate.of(1990, 5, 10), "viejo@example.test");
        ReflectionTestUtils.setField(user, "paciente", paciente);

        ActualizarPerfilRequest req = new ActualizarPerfilRequest("nuevo@example.test", "987654321", "Nueva Dirección");
        when(usuarios.findById(1L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("nuevo@example.test")).thenReturn(List.of());

        PerfilResponse resp = service.actualizar(1L, req);

        assertEquals("nuevo@example.test", paciente.getCorreo());
        assertEquals("987654321", paciente.getCelular());
        assertEquals("Nueva Dirección", paciente.getDireccion());
        verify(pacientes).save(paciente);
        assertEquals("nuevo@example.test", resp.correo());
    }

    @Test
    void actualizar_perfilRecepcionistaExitoso() {
        Usuario user = new Usuario("87654321", "hash", RolUsuario.RECEPCIONISTA);
        ReflectionTestUtils.setField(user, "id", 2L);
        Recepcionista rec = new Recepcionista(user, "Laura", "Mendoza", null, "87654321", "viejo@example.test");
        ReflectionTestUtils.setField(user, "recepcionista", rec);

        ActualizarPerfilRequest req = new ActualizarPerfilRequest("nuevo_rec@example.test", "911222333", "Nueva Sede");
        when(usuarios.findById(2L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("nuevo_rec@example.test")).thenReturn(List.of(user));

        PerfilResponse resp = service.actualizar(2L, req);

        assertEquals("nuevo_rec@example.test", rec.getCorreo());
        assertEquals("911222333", rec.getCelular());
        assertEquals("Nueva Sede", rec.getDireccion());
        verify(recepcionistas).save(rec);
        assertEquals("nuevo_rec@example.test", resp.correo());
    }

    @Test
    void actualizar_perfilAdministradorExitoso() {
        Usuario user = new Usuario("11223344", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(user, "id", 3L);
        Administrador admin = new Administrador(user, "11223344", "admin_old@example.test");
        ReflectionTestUtils.setField(user, "administrador", admin);

        ActualizarPerfilRequest req = new ActualizarPerfilRequest("admin_new@example.test", "999888777", "Oficina Central");
        when(usuarios.findById(3L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("admin_new@example.test")).thenReturn(List.of());

        PerfilResponse resp = service.actualizar(3L, req);

        assertEquals("admin_new@example.test", admin.getCorreo());
        assertEquals("999888777", admin.getCelular());
        assertEquals("Oficina Central", admin.getDireccion());
        verify(administradores).save(admin);
        assertEquals("admin_new@example.test", resp.correo());
    }

    @Test
    void actualizar_conEspaciosEnBlancoConvierteANull() {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", 1L);
        Paciente paciente = new Paciente(user, "Carlos", "Perez", "Gomez", "12345678",
                LocalDate.of(1990, 5, 10), "carlos@example.test");
        ReflectionTestUtils.setField(user, "paciente", paciente);

        ActualizarPerfilRequest req = new ActualizarPerfilRequest("carlos@example.test", "   ", "   ");
        when(usuarios.findById(1L)).thenReturn(Optional.of(user));
        when(usuarios.findByIdentifier("carlos@example.test")).thenReturn(List.of());

        PerfilResponse resp = service.actualizar(1L, req);

        assertNull(paciente.getCelular());
        assertNull(paciente.getDireccion());
        assertNull(resp.celular());
        assertNull(resp.direccion());
    }
}

