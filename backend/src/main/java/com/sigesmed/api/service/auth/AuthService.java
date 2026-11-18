package com.sigesmed.api.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.RecuperacionClave;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.dto.auth.RegistroPacienteRequest;
import com.sigesmed.api.exception.ApiException;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecuperacionClaveRepository;
import com.sigesmed.api.repository.UsuarioRepository;

@Service
public class AuthService {
    private static final int MAX_INTENTOS = 3;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final Duration RECUPERACION_TTL_DEFAULT = Duration.ofMinutes(30);
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RecuperacionClaveRepository recuperacionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecoveryLinkNotifier recoveryLinkNotifier;
    private final Clock clock;
    private final Duration recoveryTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository,
            RecuperacionClaveRepository recuperacionRepository, PasswordEncoder passwordEncoder,
            RecoveryLinkNotifier recoveryLinkNotifier, Clock clock,
            @Value("${sigesmed.auth.recovery-ttl:PT30M}") Duration recoveryTtl) {
        this.usuarioRepository = usuarioRepository;
        this.pacienteRepository = pacienteRepository;
        this.recuperacionRepository = recuperacionRepository;
        this.passwordEncoder = passwordEncoder;
        this.recoveryLinkNotifier = recoveryLinkNotifier;
        this.clock = clock;
        this.recoveryTtl = recoveryTtl == null ? RECUPERACION_TTL_DEFAULT : recoveryTtl;
    }

    @Transactional
    public Paciente registrarPaciente(RegistroPacienteRequest request) {
        String dni = request.dni().trim();
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByDni(dni) || pacienteRepository.existsByDni(dni)) {
            throw new ApiException(HttpStatus.CONFLICT, "DNI duplicado");
        }
        if (!usuarioRepository.findByIdentifier(correo).isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está asociado a otra cuenta");
        }
        Usuario usuario = usuarioRepository.save(new Usuario(
                dni, passwordEncoder.encode(request.clave()), RolUsuario.PACIENTE));
        Paciente paciente = new Paciente(usuario, request.nombre().trim(),
                request.apellidoPaterno().trim(), blankToNull(request.apellidoMaterno()),
                dni, request.fechaNacimiento(), correo);
        paciente.setCelular(blankToNull(request.celular()));
        return pacienteRepository.save(paciente);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Usuario autenticar(String identificador, String clave) {
        List<Usuario> candidates = usuarioRepository.findByIdentifier(identificador.trim());
        if (candidates.size() != 1) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        Usuario usuario = usuarioRepository.findByIdForUpdate(candidates.getFirst().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (usuario.getRecepcionista() != null && !usuario.getRecepcionista().estaActivo()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(now)) {
            throw new ApiException(HttpStatus.LOCKED, "Cuenta bloqueada");
        }
        if (!passwordEncoder.matches(clave, usuario.getPasswordHash())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            if (intentos >= MAX_INTENTOS) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(now.plus(BLOQUEO));
                usuarioRepository.save(usuario);
                throw new ApiException(HttpStatus.LOCKED, "Cuenta bloqueada por 15 minutos");
            }
            usuario.setIntentosFallidos(intentos);
            usuarioRepository.save(usuario);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);
        return usuario;
    }

    @Transactional
    public void solicitarRecuperacion(String email) {
        List<Usuario> candidates = usuarioRepository.findByIdentifier(email.trim());
        if (candidates.size() != 1)
            return;
        Usuario usuario = candidates.getFirst();
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String hash = sha256(rawToken);
        recuperacionRepository.save(new RecuperacionClave(usuario, hash,
                OffsetDateTime.now(clock).plus(recoveryTtl)));
        String accountEmail = usuario.getPaciente() != null ? usuario.getPaciente().getCorreo()
                : usuario.getRecepcionista() != null ? usuario.getRecepcionista().getCorreo()
                        : usuario.getDoctor() != null ? usuario.getDoctor().getCorreo()
                                : usuario.getAdministrador() == null ? email : usuario.getAdministrador().getCorreo();
        recoveryLinkNotifier.notify(accountEmail, rawToken);
    }

    @Transactional
    public void restablecerClave(String rawToken, String newPassword) {
        String hash = sha256(rawToken);
        RecuperacionClave token = recuperacionRepository.findValidForUpdate(hash, OffsetDateTime.now(clock))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El enlace no es válido o venció"));
        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        token.marcarUsado();
        usuarioRepository.save(usuario);
        recuperacionRepository.save(token);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
