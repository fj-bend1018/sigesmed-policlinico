package com.sigesmed.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sigesmed.api.domain.entity.Administrador;
import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    Optional<Administrador> findByUsuario_Id(Long usuarioId);
}
