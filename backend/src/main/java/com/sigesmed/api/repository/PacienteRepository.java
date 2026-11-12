package com.sigesmed.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sigesmed.api.domain.entity.Paciente;
import jakarta.persistence.LockModeType;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    boolean existsByDni(String dni);
    Optional<Paciente> findByUsuario_Id(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.usuario.id = :usuarioId")
    Optional<Paciente> findByUsuarioIdForUpdate(@Param("usuarioId") Long usuarioId);
}
