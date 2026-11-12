package com.sigesmed.api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sigesmed.api.domain.entity.ServicioComplementario;

public interface ServicioComplementarioRepository extends JpaRepository<ServicioComplementario, Long> {
    List<ServicioComplementario> findAllByOrderByNombreAsc();
}
