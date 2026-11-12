package com.sigesmed.api.domain.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor")
public class Doctor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 160) private String nombre;
    @Column(nullable = false, unique = true, length = 40) private String cmp;
    @Column(length = 500) private String imagen;
    @Column(nullable = false, length = 10) private String estado = "ACTIVO";
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "doctor_especialidad",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "especialidad_id"))
    private Set<Especialidad> especialidades = new HashSet<>();

    protected Doctor() { }
    public Doctor(String nombre, String cmp, Set<Especialidad> especialidades) {
        this.nombre = nombre; this.cmp = cmp; this.especialidades = new HashSet<>(especialidades);
    }
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCmp() { return cmp; }
    public String getEstado() { return estado; }
    public Set<Especialidad> getEspecialidades() { return especialidades; }
    public String getImagen() { return imagen; }
    public void actualizar(String nombre, String cmp, Set<Especialidad> especialidades) {
        this.nombre = nombre;
        this.cmp = cmp;
        this.especialidades = new HashSet<>(especialidades);
    }
    public void cambiarEstado(String estado) { this.estado = estado; }
}
