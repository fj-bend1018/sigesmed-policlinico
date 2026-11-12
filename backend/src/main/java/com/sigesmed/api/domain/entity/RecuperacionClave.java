package com.sigesmed.api.domain.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recuperacion_clave")
public class RecuperacionClave {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "char(64)")
    private String tokenHash;
    @Column(name = "expira_en", nullable = false) private OffsetDateTime expiraEn;
    @Column(nullable = false) private boolean usado;
    protected RecuperacionClave() { }
    public RecuperacionClave(Usuario usuario, String tokenHash, OffsetDateTime expiraEn) {
        this.usuario = usuario; this.tokenHash = tokenHash; this.expiraEn = expiraEn; this.usado = false;
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getExpiraEn() { return expiraEn; }
    public boolean isUsado() { return usado; }
    public void marcarUsado() { this.usado = true; }
}
