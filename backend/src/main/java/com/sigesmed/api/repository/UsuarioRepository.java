package com.sigesmed.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sigesmed.api.domain.entity.Usuario;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByDni(String dni);
    Optional<Usuario> findByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);

    @Query("""
        select distinct u from Usuario u
        left join u.paciente p
        left join u.recepcionista r
        left join u.administrador a
        where lower(u.dni) = lower(:identifier)
           or lower(p.correo) = lower(:identifier)
           or lower(r.correo) = lower(:identifier)
           or lower(a.correo) = lower(:identifier)
        """)
    List<Usuario> findByIdentifier(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);
}
