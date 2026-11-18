package com.sigesmed.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sigesmed.api.domain.entity.Doctor;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByIdAndEstado(Long id, String estado);
    Optional<Doctor> findByUsuario_Id(Long usuarioId);
    List<Doctor> findAllByOrderByNombreAsc();
    Optional<Doctor> findByCmpIgnoreCase(String cmp);
    boolean existsByCmpIgnoreCase(String cmp);
    boolean existsByCmpIgnoreCaseAndIdNot(String cmp, Long id);
    boolean existsByEspecialidades_Id(Long especialidadId);

    @Query("""
        select distinct d from Doctor d join d.especialidades e
        where e.id = :specialtyId and d.estado = 'ACTIVO'
        order by d.nombre
        """)
    List<Doctor> findActiveBySpecialty(@Param("specialtyId") Long specialtyId);
}
