package com.sigesmed.api.domain.entity;

import java.time.OffsetDateTime;

import com.sigesmed.api.domain.enums.EstadoCita;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cita")
public class Cita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_complementario_id")
    private ServicioComplementario servicioComplementario;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pago_id", unique = true)
    private Pago pago;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EstadoCita estado;
    @Column(name = "motivo_cancelacion", length = 500) private String motivoCancelacion;
    @Column(name = "creado_en", nullable = false) private OffsetDateTime creadoEn;
    protected Cita() { }
    public Cita(Paciente paciente, Horario horario, Especialidad especialidad,
            EstadoCita estado, OffsetDateTime creadoEn) {
        this.paciente = paciente;
        this.horario = horario;
        this.especialidad = especialidad;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }
    public Cita(Paciente paciente, Horario horario, ServicioComplementario servicioComplementario,
            EstadoCita estado, OffsetDateTime creadoEn) {
        this.paciente = paciente;
        this.horario = horario;
        this.servicioComplementario = servicioComplementario;
        this.estado = estado;
        this.creadoEn = creadoEn;
    }
    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public Horario getHorario() { return horario; }
    public Especialidad getEspecialidad() { return especialidad; }
    public ServicioComplementario getServicioComplementario() { return servicioComplementario; }
    public Pago getPago() { return pago; }
    public EstadoCita getEstado() { return estado; }
    public void setEstado(EstadoCita estado) { this.estado = estado; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public void setHorario(Horario horario) { this.horario = horario; }
    public void setEspecialidad(Especialidad especialidad) { this.especialidad = especialidad; }
    public void cancelar(String motivo) {
        this.estado = EstadoCita.CANCELADA;
        this.motivoCancelacion = motivo;
    }
}
