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
@Table(name = "administrador")
public class Administrador {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(nullable = false, unique = true, length = 8) private String dni;
    @Column(length = 254) private String correo;
    @Column(length = 30) private String celular;
    @Column(length = 300) private String direccion;

    protected Administrador() { }
    public Administrador(Usuario usuario, String dni, String correo) {
        this.usuario = usuario; this.dni = dni; this.correo = correo;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getDni() { return dni; }
    public String getCorreo() { return correo; }
    public String getCelular() { return celular; }
    public String getDireccion() { return direccion; }
    public void actualizar(String correo, String celular, String direccion) {
        this.correo = correo;
        this.celular = celular;
        this.direccion = direccion;
    }
}
