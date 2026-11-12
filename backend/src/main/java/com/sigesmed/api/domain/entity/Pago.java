package com.sigesmed.api.domain.entity;

import java.math.BigDecimal;

import com.sigesmed.api.domain.enums.EstadoPago;
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

@Entity
@Table(name = "pago")
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 40) private String metodo;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal monto;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EstadoPago estado;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "registrado_por_usuario_id")
    private Usuario registradoPor;
    protected Pago() { }
    public Long getId() { return id; }
    public EstadoPago getEstado() { return estado; }
}
