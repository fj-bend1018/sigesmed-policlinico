package com.sigesmed.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sigesmed.api.domain.entity.Recepcionista;
import java.util.List;
import java.util.Optional;

public interface RecepcionistaRepository extends JpaRepository<Recepcionista, Long> {
    List<Recepcionista> findAllByOrderByApellidoPaternoAscNombreAsc();
    Optional<Recepcionista> findByUsuario_Id(Long usuarioId);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);
    boolean existsByDni(String dni);
}
