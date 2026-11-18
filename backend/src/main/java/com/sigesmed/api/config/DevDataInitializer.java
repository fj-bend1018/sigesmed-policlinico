package com.sigesmed.api.config;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sigesmed.api.domain.entity.Administrador;
import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.entity.Recepcionista;
import com.sigesmed.api.domain.entity.ServicioComplementario;
import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;
import com.sigesmed.api.repository.AdministradorRepository;
import com.sigesmed.api.repository.CitaRepository;
import com.sigesmed.api.repository.DoctorRepository;
import com.sigesmed.api.repository.EspecialidadRepository;
import com.sigesmed.api.repository.HorarioRepository;
import com.sigesmed.api.repository.RecepcionistaRepository;
import com.sigesmed.api.repository.UsuarioRepository;
import com.sigesmed.api.repository.ServicioComplementarioRepository;

@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {
    private final UsuarioRepository usuarioRepository;
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

    public DevDataInitializer(UsuarioRepository usuarioRepository,
            RecepcionistaRepository recepcionistaRepository, AdministradorRepository administradorRepository,
            EspecialidadRepository especialidadRepository, DoctorRepository doctorRepository,
            CitaRepository citaRepository, HorarioRepository horarioRepository,
            ServicioComplementarioRepository servicioRepository, PasswordEncoder passwordEncoder, Clock clock,
            @Value("${SIGESMED_DEV_ADMIN_PASSWORD:}") String adminPassword,
            @Value("${SIGESMED_DEV_RECEPCIONISTA_PASSWORD:}") String receptionistPassword) {
        this.usuarioRepository = usuarioRepository;
        this.recepcionistaRepository = recepcionistaRepository; this.administradorRepository = administradorRepository;
        this.especialidadRepository = especialidadRepository; this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository; this.horarioRepository = horarioRepository;
        this.servicioRepository = servicioRepository;
        this.passwordEncoder = passwordEncoder; this.clock = clock;
        this.adminPassword = adminPassword; this.receptionistPassword = receptionistPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDemoAccountsIfConfigured();

        List<String> names = List.of(
                "Pediatría general",
                "Medicina familiar",
                "Nutrición",
                "Psicología infantil",
                "Odontología pediátrica",
                "Otorrinolaringología"
        );
        List<Especialidad> specialties = new ArrayList<>();
        for (String name : names) {
            specialties.add(asegurarEspecialidad(name));
        }

        // 6 profesionales por especialidad (36 en total: 3 mujeres y 3 hombres): turno mañana, tarde y completo.
        record DoctorSeed(String name, String image) {}
        List<List<DoctorSeed>> doctorsPerSpecialty = List.of(
                List.of(
                        new DoctorSeed("Dra. Carmen Rosa Salazar", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Dr. Jorge Andrés Villanueva", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Dra. Lucía Beatriz Quispe", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Dr. Álvaro Sebastián Gutiérrez", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Dra. Daniela Fernanda Ortiz", "/assets/rediseno/doc-female-3.jpg"),
                        new DoctorSeed("Dr. Mateo Nicolás Delgado", "/assets/rediseno/doc-male-3.jpg")
                ),
                List.of(
                        new DoctorSeed("Dr. Luis Alberto Paredes", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Dra. Patricia Milagros Vargas", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Dr. Miguel Ángel Navarro", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Dra. Mónica Isabel Herrera", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Dr. César Augusto Benítez", "/assets/rediseno/doc-male-3.jpg"),
                        new DoctorSeed("Dra. Camila Andrea Morales", "/assets/rediseno/doc-female-3.jpg")
                ),
                List.of(
                        new DoctorSeed("Lic. María Fernanda Chávez", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Lic. Sofía Alejandra Rojas", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Lic. Diego Alonso Campos", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Lic. Rodrigo Javier Castillo", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Lic. Natalia Rocío Espinoza", "/assets/rediseno/doc-female-3.jpg"),
                        new DoctorSeed("Lic. Sebastián Andrés Vera", "/assets/rediseno/doc-male-3.jpg")
                ),
                List.of(
                        new DoctorSeed("Ps. Carlos Eduardo Mendoza", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Ps. Valeria Lucero Flores", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Ps. Andrea Paola Castañeda", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Ps. Mauricio Enrique Lozano", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Ps. Ximena Beatriz Salazar", "/assets/rediseno/doc-female-3.jpg"),
                        new DoctorSeed("Ps. Alejandro Tomás Peña", "/assets/rediseno/doc-male-3.jpg")
                ),
                List.of(
                        new DoctorSeed("Dra. Elena Patricia Torres", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Dr. Fernando José Aguilar", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Dra. Gabriela Sofía Huamán", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Dr. Ernesto Rafael Córdova", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Dra. Paola Cristina Medina", "/assets/rediseno/doc-female-3.jpg"),
                        new DoctorSeed("Dr. Gonzalo Iván Ramírez", "/assets/rediseno/doc-male-3.jpg")
                ),
                List.of(
                        new DoctorSeed("Dr. Roberto Antonio Ríos", "/assets/rediseno/doc-male-1.jpg"),
                        new DoctorSeed("Dra. Claudia Verónica Silva", "/assets/rediseno/doc-female-1.jpg"),
                        new DoctorSeed("Dr. Ricardo Manuel León", "/assets/rediseno/doc-male-2.jpg"),
                        new DoctorSeed("Dra. Verónica Lucía Mendoza", "/assets/rediseno/doc-female-2.jpg"),
                        new DoctorSeed("Dr. Héctor Darío Cárdenas", "/assets/rediseno/doc-male-3.jpg"),
                        new DoctorSeed("Dra. Teresa Milagros Núñez", "/assets/rediseno/doc-female-3.jpg")
                )
        );

        List<LocalTime> morningHours = List.of(
                LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(10, 0),
                LocalTime.of(11, 0), LocalTime.of(12, 0)
        );
        List<LocalTime> afternoonHours = List.of(
                LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0),
                LocalTime.of(17, 0), LocalTime.of(18, 0), LocalTime.of(19, 0)
        );
        List<LocalTime> fullHours = new ArrayList<>();
        fullHours.addAll(morningHours);
        fullHours.addAll(afternoonHours);

        List<Doctor> doctors = new ArrayList<>();
        List<List<LocalTime>> doctorShiftHours = new ArrayList<>();

        // Mantener los 6 CMP base (0001-0006) para el primer médico de cada especialidad
        for (int specIdx = 0; specIdx < specialties.size(); specIdx++) {
            String cmp = "DEV-CMP-%04d".formatted(specIdx + 1);
            DoctorSeed seed = doctorsPerSpecialty.get(specIdx).get(0);
            Especialidad spec = specialties.get(specIdx);
            Doctor doctor = doctorRepository.findByCmpIgnoreCase(cmp)
                    .orElseGet(() -> new Doctor(seed.name(), cmp, Set.of(spec)));
            doctor.actualizar(seed.name(), cmp, Set.of(spec));
            doctor.asignarImagen(seed.image());
            doctors.add(doctorRepository.save(doctor));
            doctorShiftHours.add(morningHours);
        }

        // Mantener 0007-0018 para el 2.º y 3.º médico de cada especialidad, y 0019-0036 para los 3 adicionales
        int nextCmpIndex = 7;
        for (int specIdx = 0; specIdx < specialties.size(); specIdx++) {
            Especialidad spec = specialties.get(specIdx);
            for (int tier = 1; tier <= 2; tier++) {
                String cmp = "DEV-CMP-%04d".formatted(nextCmpIndex++);
                DoctorSeed seed = doctorsPerSpecialty.get(specIdx).get(tier);
                Doctor doctor = doctorRepository.findByCmpIgnoreCase(cmp)
                        .orElseGet(() -> new Doctor(seed.name(), cmp, Set.of(spec)));
                doctor.actualizar(seed.name(), cmp, Set.of(spec));
                doctor.asignarImagen(seed.image());
                doctors.add(doctorRepository.save(doctor));
                doctorShiftHours.add(tier == 1 ? afternoonHours : fullHours);
            }
        }
        for (int specIdx = 0; specIdx < specialties.size(); specIdx++) {
            Especialidad spec = specialties.get(specIdx);
            for (int tier = 3; tier < 6; tier++) {
                String cmp = "DEV-CMP-%04d".formatted(nextCmpIndex++);
                DoctorSeed seed = doctorsPerSpecialty.get(specIdx).get(tier);
                Doctor doctor = doctorRepository.findByCmpIgnoreCase(cmp)
                        .orElseGet(() -> new Doctor(seed.name(), cmp, Set.of(spec)));
                doctor.actualizar(seed.name(), cmp, Set.of(spec));
                doctor.asignarImagen(seed.image());
                doctors.add(doctorRepository.save(doctor));
                doctorShiftHours.add(tier == 3 ? morningHours : (tier == 4 ? afternoonHours : fullHours));
            }
        }

        remapLegacySpecialties();

        LocalDate firstDay = LocalDate.now(clock).plusDays(1);
        LocalDate lastDay = firstDay.plusDays(6);
        List<ServicioComplementario> services = servicioRepository.findAllByOrderByNombreAsc();
        List<Horario> existingWeek = horarioRepository.findAllByFechaBetweenOrderByFechaAscHoraAsc(firstDay, lastDay);
        Set<String> existingKeys = new HashSet<>();
        for (Horario h : existingWeek) {
            if (h.getDoctor() != null) {
                existingKeys.add("D:" + h.getDoctor().getId() + ":" + h.getFecha() + ":" + h.getHora());
            } else if (h.getServicioComplementario() != null) {
                existingKeys.add("S:" + h.getServicioComplementario().getId() + ":" + h.getFecha() + ":" + h.getHora());
            }
        }

        List<Horario> pendingSchedules = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = firstDay.plusDays(day);
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            for (int d = 0; d < doctors.size(); d++) {
                Doctor doctor = doctors.get(d);
                for (LocalTime time : doctorShiftHours.get(d)) {
                    String key = "D:" + doctor.getId() + ":" + date + ":" + time;
                    if (!existingKeys.contains(key)) {
                        pendingSchedules.add(new Horario(doctor, date, time));
                    }
                }
            }
            for (ServicioComplementario service : services) {
                for (LocalTime time : fullHours) {
                    String key = "S:" + service.getId() + ":" + date + ":" + time;
                    if (!existingKeys.contains(key)) {
                        pendingSchedules.add(new Horario(service, date, time));
                    }
                }
            }
        }
        if (!pendingSchedules.isEmpty()) {
            horarioRepository.saveAll(pendingSchedules);
        }
    }

    private void seedDemoAccountsIfConfigured() {
        boolean anyPassword = !adminPassword.isBlank() || !receptionistPassword.isBlank();
        boolean allPasswords = !adminPassword.isBlank() && !receptionistPassword.isBlank();
        if (anyPassword && !allPasswords) {
            throw new IllegalStateException("Defina SIGESMED_DEV_ADMIN_PASSWORD y SIGESMED_DEV_RECEPCIONISTA_PASSWORD o ninguna.");
        }

        boolean staffAccountsExist = usuarioRepository.findByDni("74829105").isPresent()
                && usuarioRepository.findByDni("48291637").isPresent();
        if (!allPasswords && !staffAccountsExist) return;

        asegurarCuenta("74829105", RolUsuario.ADMINISTRADOR, adminPassword, "SIGESMED_DEV_ADMIN_PASSWORD");
        asegurarCuenta("48291637", RolUsuario.RECEPCIONISTA, receptionistPassword,
                "SIGESMED_DEV_RECEPCIONISTA_PASSWORD");

        Usuario admin = usuarioRepository.findByDni("74829105").orElseThrow();
        administradorRepository.findByUsuario_Id(admin.getId())
                .orElseGet(() -> administradorRepository.save(new Administrador(admin, "74829105", "admin@policlinico.pe")));
        Usuario receptionist = usuarioRepository.findByDni("48291637").orElseThrow();
        recepcionistaRepository.findByUsuario_Id(receptionist.getId())
                .orElseGet(() -> recepcionistaRepository.save(new Recepcionista(receptionist, "Rosa", "Mendoza", "Díaz",
                        "48291637", "recep@policlinico.pe")));

        // Vincular cuenta de acceso para Dra. Carmen Rosa Salazar (Portal del Médico)
        doctorRepository.findByCmpIgnoreCase("DEV-CMP-0001").ifPresent(doc -> {
            if (doc.getUsuario() == null && usuarioRepository.findByDni("62948173").isEmpty()) {
                Usuario docUser = usuarioRepository.save(
                        new Usuario("62948173", passwordEncoder.encode("doc123"), RolUsuario.DOCTOR));
                doc.vincularUsuario(docUser, "carmen.salazar@policlinico.pe");
                doctorRepository.save(doc);
            }
        });
    }

    private Usuario asegurarCuenta(String dni, RolUsuario rol, String password, String variable) {
        Optional<Usuario> existing = usuarioRepository.findByDni(dni);
        if (existing.isPresent()) {
            if (password != null && !password.isBlank()) {
                Usuario u = existing.get();
                u.setPasswordHash(passwordEncoder.encode(password));
                return usuarioRepository.save(u);
            }
            return existing.get();
        }
        requirePassword(password, variable);
        return usuarioRepository.save(new Usuario(dni, passwordEncoder.encode(password), rol));
    }

    private Especialidad asegurarEspecialidad(String nombre) {
        return especialidadRepository.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> especialidadRepository.save(new Especialidad(nombre)));
    }

    private void remapLegacySpecialties() {
        remapLegacySpecialty("Pediatría", "Pediatría general");
        remapLegacySpecialty("Medicina General", "Medicina familiar");
        remapLegacySpecialty("Odontología", "Odontología pediátrica");

        List<String> toClean = List.of("Cardiología", "Medicina Interna", "Traumatología",
                "Pediatría", "Medicina General", "Odontología");
        for (String legacy : toClean) {
            especialidadRepository.findByNombreIgnoreCase(legacy).ifPresent(oldSpecialty -> {
                if (!doctorRepository.existsByEspecialidades_Id(oldSpecialty.getId())
                        && !citaRepository.existsByEspecialidad_Id(oldSpecialty.getId())) {
                    especialidadRepository.delete(oldSpecialty);
                }
            });
        }
    }

    private void remapLegacySpecialty(String legacyName, String officialName) {
        especialidadRepository.findByNombreIgnoreCase(legacyName).ifPresent(oldSpecialty -> {
            Especialidad target = asegurarEspecialidad(officialName);
            List<Cita> citas = citaRepository.findAll().stream()
                    .filter(c -> c.getEspecialidad() != null && c.getEspecialidad().getId().equals(oldSpecialty.getId()))
                    .collect(Collectors.toList());
            for (Cita c : citas) {
                c.setEspecialidad(target);
                citaRepository.save(c);
            }
            if (!doctorRepository.existsByEspecialidades_Id(oldSpecialty.getId())) {
                especialidadRepository.delete(oldSpecialty);
            }
        });
    }

    private void requirePassword(String password, String variable) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Defina " + variable + " para activar datos de desarrollo");
        }
    }
}
