package com.sigesmed.api.domain.entity;

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
@Table(name = "recepcionista")
public class Recepcionista {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(nullable = false, length = 100) private String nombre;
    @Column(name = "apellido_paterno", nullable = false, length = 100) private String apellidoPaterno;
    @Column(name = "apellido_materno", length = 100) private String apellidoMaterno;
    @Column(nullable = false, unique = true, length = 8) private String dni;
    @Column(nullable = false, unique = true, length = 254) private String correo;
    @Column(length = 30) private String celular;
    @Column(length = 300) private String direccion;
    @Column(nullable = false, length = 10) private String estado = "ACTIVO";

    protected Recepcionista() { }
    public Recepcionista(Usuario usuario, String nombre, String apellidoPaterno, String apellidoMaterno,
            String dni, String correo) {
        this.usuario = usuario; this.nombre = nombre; this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno; this.dni = dni; this.correo = correo;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public String getDni() { return dni; }
    public String getCorreo() { return correo; }
    public String getCelular() { return celular; }
    public String getDireccion() { return direccion; }
    public String getEstado() { return estado; }
    public void actualizar(String nombre, String apellidoPaterno, String apellidoMaterno,
            String correo, String celular, String direccion) {
        this.nombre = nombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.correo = correo;
        this.celular = celular;
        this.direccion = direccion;
    }
    public void cambiarEstado(String estado) { this.estado = estado; }
    public boolean estaActivo() { return "ACTIVO".equals(estado); }
}
