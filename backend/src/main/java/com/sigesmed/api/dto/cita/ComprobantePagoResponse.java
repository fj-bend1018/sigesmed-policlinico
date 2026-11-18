package com.sigesmed.api.dto.cita;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.entity.Doctor;
import com.sigesmed.api.domain.entity.Especialidad;
import com.sigesmed.api.domain.entity.Pago;
import com.sigesmed.api.domain.enums.EstadoCita;
import com.sigesmed.api.domain.enums.EstadoPago;

public record ComprobantePagoResponse(
        Long pagoId,
        Long citaId,
        String codigoComprobante,
        String metodo,
        BigDecimal monto,
        EstadoPago estadoPago,
        EstadoCita estadoCita,
        String paciente,
        String pacienteDni,
        String atencion,
        String doctor,
        LocalDate fecha,
        LocalTime hora,
        String registradoPorDni) {

    public static ComprobantePagoResponse from(Cita cita, Pago pago, String registradoPorDni) {
        Doctor doctor = cita.getHorario().getDoctor();
        String atencion = cita.getServicioComplementario() != null
                ? cita.getServicioComplementario().getNombre()
                : cita.getEspecialidad() != null
                        ? cita.getEspecialidad().getNombre()
                        : doctor == null ? "" : doctor.getEspecialidades().stream()
                                .map(Especialidad::getNombre).sorted().findFirst().orElse("");
        String paciente = cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidoPaterno();
        String codigo = "CMP-PAGO-" + String.format("%06d", pago.getId() == null ? 0L : pago.getId());
        return new ComprobantePagoResponse(
                pago.getId(),
                cita.getId(),
                codigo,
                pago.getMetodo(),
                pago.getMonto(),
                pago.getEstado(),
                cita.getEstado(),
                paciente,
                cita.getPaciente().getDni(),
                atencion,
                doctor == null ? null : doctor.getNombre(),
                cita.getHorario().getFecha(),
                cita.getHorario().getHora(),
                registradoPorDni);
    }
}
