package com.sigesmed.api.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sigesmed.api.domain.entity.Usuario;

public final class UsuarioPrincipal implements UserDetails {
    private final Long id;
    private final String dni;
    private final String role;

    public UsuarioPrincipal(Usuario usuario) {
        this.id = usuario.getId();
        this.dni = usuario.getDni();
        this.role = usuario.getRol().name();
    }

    public Long getId() { return id; }
    public String getDni() { return dni; }
    public String getRole() { return role; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return dni; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
