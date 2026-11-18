package com.sigesmed.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sigesmed.api.domain.entity.Usuario;
import com.sigesmed.api.domain.enums.RolUsuario;

class UsuarioPrincipalTest {

    @Test
    void exponeDatosYAutoridadesDelUsuario() {
        Usuario u = new Usuario("70000001", "hash", RolUsuario.DOCTOR);
        ReflectionTestUtils.setField(u, "id", 42L);
        UsuarioPrincipal p = new UsuarioPrincipal(u);

        assertEquals(42L, p.getId());
        assertEquals("70000001", p.getDni());
        assertEquals("DOCTOR", p.getRole());
        assertEquals("70000001", p.getUsername());
        assertEquals("", p.getPassword());
        assertEquals("ROLE_DOCTOR", p.getAuthorities().iterator().next().getAuthority());
        assertTrue(p.isAccountNonExpired());
        assertTrue(p.isAccountNonLocked());
        assertTrue(p.isCredentialsNonExpired());
        assertTrue(p.isEnabled());
    }
}
