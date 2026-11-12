package com.sigesmed.api.config;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Administrador;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Paciente;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.repository.AdministradorRepository;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.PacienteRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;

@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {
    private final UsuarioRepository usuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final RecepcionistaRepository recepcionistaRepository;
    private final AdministradorRepository administradorRepository;
    private final EspecialidadRepository especialidadRepository;
    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final ServicioComplementarioRepository servicioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String adminPassword;
    private final String receptionistPassword;
    private final String patientPassword;

    public DevDataInitializer(UsuarioRepository usuarioRepository, PacienteRepository pacienteRepository,
            RecepcionistaRepository recepcionistaRepository, AdministradorRepository administradorRepository,
            EspecialidadRepository especialidadRepository, DoctorRepository doctorRepository,
            CitaRepository citaRepository, HorarioRepository horarioRepository,
            ServicioComplementarioRepository servicioRepository, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${SIGESMED_DEV_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${SIGESMED_DEV_RECEPCIONISTA_PASSWORD:}") String receptionistPassword,
            @Value("${SIGESMED_DEV_PACIENTE_PASSWORD:}") String patientPassword) {
        this.usuarioRepository = usuarioRepository; this.pacienteRepository = pacienteRepository;
        this.recepcionistaRepository = recepcionistaRepository; this.administradorRepository = administradorRepository;
        this.especialidadRepository = especialidadRepository; this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository; this.horarioRepository = horarioRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder; this.clock = clock;
        this.adminPassword = adminPassword; this.receptionistPassword = receptionistPassword;
        this.patientPassword = patientPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDemoAccountsIfConfigured();

        List<String> names = List.of("Pediatría", "Medicina General", "Odontología",
                "Medicina Interna", "Traumatología");
        List<String> doctorNames = List.of("Dra. Ana Demo", "Dr. Luis Demo", "Dra. María Demo",
                "Dr. Carlos Demo", "Dra. Elena Demo");
        List<Especialidad> specialties = names.stream().map(this::asegurarEspecialidad).toList();
        List<Doctor> doctors = new ArrayList<>();
        for (int i = 0; i < specialties.size(); i++) {
            int index = i;
            String cmp = "DEV-CMP-%04d".formatted(index + 1);
            Doctor doctor = doctorRepository.findByCmpIgnoreCase(cmp)
                    .orElseGet(() -> new Doctor(doctorNames.get(index), cmp, Set.of(specialties.get(index))));
            doctor.actualizar(doctorNames.get(index), cmp, Set.of(specialties.get(index)));
            doctors.add(doctorRepository.save(doctor));
        }

        especialidadRepository.findByNombreIgnoreCase("Cardiología").ifPresent(oldSpecialty -> {
            if (!doctorRepository.existsByEspecialidades_Id(oldSpecialty.getId())
                    && !citaRepository.existsByEspecialidad_Id(oldSpecialty.getId())) {
                especialidadRepository.delete(oldSpecialty);
            }
        });

        LocalDate firstDay = LocalDate.now(clock).plusDays(1);
        List<ServicioComplementario> services = servicioRepository.findAllByOrderByNombreAsc();
        for (int day = 0; day < 7; day++) {
            LocalDate date = firstDay.plusDays(day);
            for (Doctor doctor : doctors) {
                for (LocalTime time : List.of(LocalTime.of(9, 0), LocalTime.of(10, 0))) {
                    if (!horarioRepository.existsByDoctor_IdAndFechaAndHora(doctor.getId(), date, time)) {
                        horarioRepository.save(new Horario(doctor, date, time));
                    }
                }
            }
            for (ServicioComplementario service : services) {
                for (LocalTime time : List.of(LocalTime.of(9, 0), LocalTime.of(10, 0))) {
                    if (!horarioRepository.existsByServicioComplementario_IdAndFechaAndHora(
                            service.getId(), date, time)) {
                        horarioRepository.save(new Horario(service, date, time));
                    }
                }
            }
        }
    }

    private void seedDemoAccountsIfConfigured() {
        boolean anyPassword = !adminPassword.isBlank() || !receptionistPassword.isBlank() || !patientPassword.isBlank();
        boolean allPasswords = !adminPassword.isBlank() && !receptionistPassword.isBlank() && !patientPassword.isBlank();
        if (anyPassword && !allPasswords) {
            throw new IllegalStateException("Defina las tres variables SIGESMED_DEV_*_PASSWORD o ninguna.");
        }

        boolean allAccountsExist = usuarioRepository.findByDni("90000001").isPresent()
                && usuarioRepository.findByDni("90000002").isPresent()
                && usuarioRepository.findByDni("90000003").isPresent();
        if (!allPasswords && !allAccountsExist) return;

        asegurarCuenta("90000001", RolUsuario.ADMINISTRADOR, adminPassword, "SIGESMED_DEV_ADMIN_PASSWORD");
        asegurarCuenta("90000002", RolUsuario.RECEPCIONISTA, receptionistPassword,
                "SIGESMED_DEV_RECEPCIONISTA_PASSWORD");
        asegurarCuenta("90000003", RolUsuario.PACIENTE, patientPassword, "SIGESMED_DEV_PACIENTE_PASSWORD");

        Usuario admin = usuarioRepository.findByDni("90000001").orElseThrow();
        administradorRepository.findByUsuario_Id(admin.getId())
                .orElseGet(() -> administradorRepository.save(new Administrador(admin, "90000001", "admin@example.test")));
        Usuario receptionist = usuarioRepository.findByDni("90000002").orElseThrow();
        recepcionistaRepository.findByUsuario_Id(receptionist.getId())
                .orElseGet(() -> recepcionistaRepository.save(new Recepcionista(receptionist, "Rosa", "Recepción", "Demo",
                        "90000002", "recepcion@example.test")));
        Usuario patientUser = usuarioRepository.findByDni("90000003").orElseThrow();
        pacienteRepository.findByUsuario_Id(patientUser.getId())
                .orElseGet(() -> pacienteRepository.save(new Paciente(patientUser, "Paciente", "Prueba", "Demo", "90000003",
                        LocalDate.of(2000, 1, 1), "paciente@example.test")));
    }

    private Usuario asegurarCuenta(String dni, RolUsuario rol, String password, String variable) {
        Optional<Usuario> existing = usuarioRepository.findByDni(dni);
        if (existing.isPresent()) return existing.get();
        requirePassword(password, variable);
        return usuarioRepository.save(new Usuario(dni, passwordEncoder.encode(password), rol));
    }

    private Especialidad asegurarEspecialidad(String nombre) {
        return especialidadRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> especialidadRepository.save(new Especialidad(nombre)));
    }

    private void requirePassword(String password, String variable) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Defina " + variable + " para activar datos de desarrollo");
        }
    }
}
