package com.sigesmed.api.domain.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import com.sigesmed.api.domain.enums.EstadoHorario;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "horario", uniqueConstraints = @UniqueConstraint(columnNames = { "doctor_id", "fecha", "hora" }))
public class Horario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "servicio_complementario_id")
    private ServicioComplementario servicioComplementario;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false) private LocalTime hora;
    @Column(nullable = false) private boolean disponible = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private EstadoHorario estado = EstadoHorario.DISPONIBLE;

    protected Horario() { }
    public Horario(Doctor doctor, LocalDate fecha, LocalTime hora) {
        this.doctor = doctor; this.fecha = fecha; this.hora = hora;
        this.disponible = true; this.estado = EstadoHorario.DISPONIBLE;
    }
    public Horario(ServicioComplementario servicioComplementario, LocalDate fecha, LocalTime hora) {
        this.servicioComplementario = servicioComplementario; this.fecha = fecha; this.hora = hora;
        this.disponible = true; this.estado = EstadoHorario.DISPONIBLE;
    }
    public Long getId() { return id; }
    public Doctor getDoctor() { return doctor; }
    public ServicioComplementario getServicioComplementario() { return servicioComplementario; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHora() { return hora; }
    public boolean isDisponible() { return disponible; }
    public EstadoHorario getEstado() { return estado; }
    public void ocupar() { this.disponible = false; this.estado = EstadoHorario.OCUPADO; }
    public void liberar() { this.disponible = true; this.estado = EstadoHorario.DISPONIBLE; }
    public void actualizar(Doctor doctor, LocalDate fecha, LocalTime hora) {
        this.doctor = doctor;
        this.servicioComplementario = null;
        this.fecha = fecha;
        this.hora = hora;
    }
    public void actualizar(ServicioComplementario servicioComplementario, LocalDate fecha, LocalTime hora) {
        this.doctor = null;
        this.servicioComplementario = servicioComplementario;
        this.fecha = fecha;
        this.hora = hora;
    }
}
