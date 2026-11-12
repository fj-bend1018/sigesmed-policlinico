package com.sigesmed.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sigesmed.api.domain.entity.Especialidad;

public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {
    List<Especialidad> findAllByOrderByNombreAsc();
    Optional<Especialidad> findByNombreIgnoreCase(String nombre);
}
