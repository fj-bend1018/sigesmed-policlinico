package com.sigesmed.api.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.RecuperacionClave;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecuperacionClaveRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.ofInstant(
            Instant.parse("2026-11-08T15:00:00Z"), ZoneId.of("America/Lima"));
    private final Clock clock = Clock.fixed(NOW.toInstant(), ZoneId.of("America/Lima"));
    @Mock private UsuarioRepository usuarios;
    @Mock private PacienteRepository pacientes;
    @Mock private RecuperacionClaveRepository recuperaciones;
    @Mock private PasswordEncoder encoder;
    @Mock private RecoveryLinkNotifier notifier;
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(usuarios, pacientes, recuperaciones, encoder, notifier,
                clock, Duration.ofMinutes(30));
    }

    @Test
    void autenticar_C1_usuarioNoExiste() {
        when(usuarios.findByIdentifier("noexiste@example.test")).thenReturn(List.of());
        ApiException exception = assertThrows(ApiException.class,
                () -> service.autenticar("noexiste@example.test", "clave"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(usuarios, never()).findByIdForUpdate(any());
    }

    @Test
    void autenticar_C2_cuentaBloqueada() {
        Usuario user = user(3L, 0);
        user.setBloqueadoHasta(NOW.plusMinutes(2));
        when(usuarios.findByIdentifier("12345678")).thenReturn(List.of(user));
        when(usuarios.findByIdForUpdate(3L)).thenReturn(Optional.of(user));
        ApiException exception = assertThrows(ApiException.class,
                () -> service.autenticar("12345678", "clave"));
        assertEquals(HttpStatus.LOCKED, exception.getStatus());
        assertEquals("Cuenta bloqueada", exception.getMessage());
        verify(encoder, never()).matches(any(), any());
    }

    @Test
    void autenticar_C3_claveIncorrectaIncrementaIntentos() {
        Usuario user = user(4L, 0);
        when(usuarios.findByIdentifier("12345678")).thenReturn(List.of(user));
        when(usuarios.findByIdForUpdate(4L)).thenReturn(Optional.of(user));
        when(encoder.matches("incorrecta", "hash")).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class,
                () -> service.autenticar("12345678", "incorrecta"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals(1, user.getIntentosFallidos());
        assertEquals(null, user.getBloqueadoHasta());
        verify(usuarios).save(user);
    }

    @Test
    void autenticar_C4_tercerIntentoBloquea15Minutos() {
        Usuario user = user(5L, 2);
        when(usuarios.findByIdentifier("12345678")).thenReturn(List.of(user));
        when(usuarios.findByIdForUpdate(5L)).thenReturn(Optional.of(user));
        when(encoder.matches("incorrecta", "hash")).thenReturn(false);
        ApiException exception = assertThrows(ApiException.class,
                () -> service.autenticar("12345678", "incorrecta"));
        assertEquals(HttpStatus.LOCKED, exception.getStatus());
        assertEquals(0, user.getIntentosFallidos());
        assertEquals(NOW.plusMinutes(15), user.getBloqueadoHasta());
        verify(usuarios).save(user);
    }

    @Test
    void autenticar_C5_claveCorrectaReiniciaContadores() {
        Usuario user = user(6L, 2);
        user.setBloqueadoHasta(NOW.minusMinutes(1));
        when(usuarios.findByIdentifier("12345678")).thenReturn(List.of(user));
        when(usuarios.findByIdForUpdate(6L)).thenReturn(Optional.of(user));
        when(encoder.matches("correcta", "hash")).thenReturn(true);
        Usuario result = service.autenticar("12345678", "correcta");
        assertEquals(user, result);
        assertEquals(0, user.getIntentosFallidos());
        assertEquals(null, user.getBloqueadoHasta());
        verify(usuarios).save(user);
    }

    @Test
    void autenticar_C6_recepcionistaInactivo() {
        Usuario user = user(8L, 0);
        user.setRol(RolUsuario.RECEPCIONISTA);
        Recepcionista rec = new Recepcionista(user, "Ana", "Recepcion", null, "12345678", "ana@example.test");
        rec.cambiarEstado("INACTIVO");
        ReflectionTestUtils.setField(user, "recepcionista", rec);

        when(usuarios.findByIdentifier("12345678")).thenReturn(List.of(user));
        when(usuarios.findByIdForUpdate(8L)).thenReturn(Optional.of(user));

        ApiException exception = assertThrows(ApiException.class,
                () -> service.autenticar("12345678", "clave"));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Credenciales inválidas", exception.getMessage());
        verify(encoder, never()).matches(any(), any());
    }

    @Test
    void solicitarRecuperacion_noHaceNadaSiUsuarioNoExiste() {
        when(usuarios.findByIdentifier("nadie@example.test")).thenReturn(List.of());
        service.solicitarRecuperacion("nadie@example.test");
        verify(recuperaciones, never()).save(any());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void restablecerClave_rechazaTokenInvalidoOVencido() {
        when(recuperaciones.findValidForUpdate(any(), any())).thenReturn(Optional.empty());
        ApiException exception = assertThrows(ApiException.class,
                () -> service.restablecerClave("tokenInvalido", "NuevaClave123!"));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("El enlace no es válido o venció", exception.getMessage());
    }

    @Test
    void restablecerClave_exitosoActualizaPasswordYMarcaUsado() {
        Usuario user = user(9L, 2);
        user.setBloqueadoHasta(NOW.plusMinutes(5));
        RecuperacionClave token = new RecuperacionClave(user, "hashToken", NOW.plusMinutes(30));
        when(recuperaciones.findValidForUpdate(any(), any())).thenReturn(Optional.of(token));
        when(encoder.encode("NuevaClave123!")).thenReturn("newHashedPassword");

        service.restablecerClave("rawToken", "NuevaClave123!");

        assertEquals("newHashedPassword", user.getPasswordHash());
        assertEquals(0, user.getIntentosFallidos());
        assertEquals(null, user.getBloqueadoHasta());
        assertEquals(true, token.isUsado());
        verify(usuarios).save(user);
        verify(recuperaciones).save(token);
    }

    @Test
    void recuperar_guardaSoloHashDelToken() {
        Usuario user = user(7L, 0);
        when(usuarios.findByIdentifier("paciente@example.test")).thenReturn(List.of(user));
        ArgumentCaptor<RecuperacionClave> tokenCaptor = ArgumentCaptor.forClass(RecuperacionClave.class);
        service.solicitarRecuperacion("paciente@example.test");
        verify(recuperaciones).save(tokenCaptor.capture());
        String tokenHash = tokenCaptor.getValue().getTokenHash();
        assertEquals(64, tokenHash.length());
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(notifier).notify(eq("paciente@example.test"), rawTokenCaptor.capture());
        assertNotEquals(rawTokenCaptor.getValue(), tokenHash);
    }

    @Test
    void registrarPaciente_rechazaDniDuplicado() {
        com.sigesmed.api.dto.auth.RegistroPacienteRequest request = new com.sigesmed.api.dto.auth.RegistroPacienteRequest(
                "Juan", "Perez", "Gomez", "12345678", java.time.LocalDate.of(1995, 5, 20),
                "juan@example.test", "ClaveValida123!");
        when(usuarios.existsByDni("12345678")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.registrarPaciente(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("DNI duplicado", ex.getMessage());
        verify(usuarios, never()).save(any());
    }

    @Test
    void registrarPaciente_rechazaCorreoDuplicado() {
        com.sigesmed.api.dto.auth.RegistroPacienteRequest request = new com.sigesmed.api.dto.auth.RegistroPacienteRequest(
                "Juan", "Perez", "Gomez", "87654321", java.time.LocalDate.of(1995, 5, 20),
                "existente@example.test", "ClaveValida123!");
        when(usuarios.existsByDni("87654321")).thenReturn(false);
        when(pacientes.existsByDni("87654321")).thenReturn(false);
        when(usuarios.findByIdentifier("existente@example.test")).thenReturn(List.of(user(10L, 0)));

        ApiException ex = assertThrows(ApiException.class, () -> service.registrarPaciente(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("El correo ya está asociado a otra cuenta", ex.getMessage());
        verify(usuarios, never()).save(any());
    }

    @Test
    void registrarPaciente_exitoso() {
        com.sigesmed.api.dto.auth.RegistroPacienteRequest request = new com.sigesmed.api.dto.auth.RegistroPacienteRequest(
                "Nuevo", "Paciente", "Prueba", "88889999", java.time.LocalDate.of(1998, 3, 15),
                "nuevo@example.test", "ClaveValida123!");
        when(usuarios.existsByDni("88889999")).thenReturn(false);
        when(pacientes.existsByDni("88889999")).thenReturn(false);
        when(usuarios.findByIdentifier("nuevo@example.test")).thenReturn(List.of());
        when(encoder.encode("ClaveValida123!")).thenReturn("hashBcrypt");
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(pacientes.save(any(com.sigesmed.api.domain.entity.Paciente.class))).thenAnswer(i -> i.getArgument(0));

        com.sigesmed.api.domain.entity.Paciente result = service.registrarPaciente(request);
        assertEquals("88889999", result.getDni());
        assertEquals("nuevo@example.test", result.getCorreo());
        verify(usuarios).save(any());
        verify(pacientes).save(any());
    }

    private Usuario user(Long id, int attempts) {
        Usuario user = new Usuario("12345678", "hash", RolUsuario.PACIENTE);
        ReflectionTestUtils.setField(user, "id", id);
        user.setIntentosFallidos(attempts);
        return user;
    }
}
