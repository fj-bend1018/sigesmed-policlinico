package com.sigesmed.api.domain.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "paciente")
public class Paciente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(name = "apellido_paterno", nullable = false, length = 100)
    private String apellidoPaterno;
    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;
    @Column(nullable = false, unique = true, length = 8)
    private String dni;
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    @Column(length = 30)
    private String sexo;
    @Column(length = 254)
    private String correo;
    @Column(length = 30)
    private String celular;
    @Column(length = 300)
    private String direccion;

    protected Paciente() { }
    public Paciente(Usuario usuario, String nombre, String apellidoPaterno, String apellidoMaterno,
                    String dni, LocalDate fechaNacimiento, String correo) {
        this.usuario = usuario; this.nombre = nombre; this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno; this.dni = dni; this.fechaNacimiento = fechaNacimiento;
        this.correo = correo;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getDni() { return dni; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}
