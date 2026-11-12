package com.sigesmed.api.dto.cita;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.enums.EstadoCita;

public record CitaResponse(Long id, Long horarioId, Long doctorId, EstadoCita estado, String paciente, String pacienteDni,
        String correoPaciente, List<String> especialidades, String doctor, LocalDate fecha, LocalTime hora,
        String motivoCancelacion, Long servicioId, String servicio) {
    public static CitaResponse from(Cita cita) {
        Doctor doctor = cita.getHorario().getDoctor();
        boolean complementaryService = cita.getServicioComplementario() != null;
        List<String> specialties = complementaryService ? List.of() : cita.getEspecialidad() == null
                ? doctor.getEspecialidades().stream().map(Especialidad::getNombre).sorted().toList()
                : List.of(cita.getEspecialidad().getNombre());
        String patientName = cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidoPaterno();
        return new CitaResponse(cita.getId(), cita.getHorario().getId(), doctor == null ? null : doctor.getId(),
                cita.getEstado(), patientName, cita.getPaciente().getDni(), cita.getPaciente().getCorreo(), specialties,
                doctor == null ? null : doctor.getNombre(), cita.getHorario().getFecha(), cita.getHorario().getHora(),
                cita.getMotivoCancelacion(), complementaryService ? cita.getServicioComplementario().getId() : null,
                complementaryService ? cita.getServicioComplementario().getNombre() : null);
    }
}
