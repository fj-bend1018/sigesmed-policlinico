package com.sigesmed.api.domain.entity;

import java.time.OffsetDateTime;

import com.sigesmed.api.domain.enums.RolUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 8)
    private String dni;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolUsuario rol;
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;
    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Paciente paciente;
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Recepcionista recepcionista;
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Administrador administrador;
    @OneToOne(mappedBy = "usuario", fetch = FetchType.LAZY)
    private Doctor doctor;

    protected Usuario() { }

    public Usuario(String dni, String passwordHash, RolUsuario rol) {
        this.dni = dni;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public RolUsuario getRol() { return rol; }
    public void setRol(RolUsuario rol) { this.rol = rol; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public OffsetDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(OffsetDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public Paciente getPaciente() { return paciente; }
    public Recepcionista getRecepcionista() { return recepcionista; }
    public Administrador getAdministrador() { return administrador; }
    public Doctor getDoctor() { return doctor; }
}
