package com.sigesmed.api.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sigesmed.api.domain.entity.Cita;
import com.sigesmed.api.domain.enums.EstadoCita;
import jakarta.persistence.LockModeType;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    boolean existsByHorario_IdAndEstadoIn(Long horarioId, Collection<EstadoCita> estados);

    @Query("""
        select case when count(c) > 0 then true else false end
        from Cita c
        where c.paciente.id = :patientId
          and c.horario.fecha = :date and c.horario.hora = :time
          and c.estado in :activeStates
        """)
    boolean existsActiveForPatientAt(@Param("patientId") Long patientId,
            @Param("date") LocalDate date, @Param("time") LocalTime time,
            @Param("activeStates") Collection<EstadoCita> activeStates);

    @Query("""
        select c.horario.hora from Cita c
        where c.paciente.usuario.id = :userId
          and c.horario.fecha = :date and c.estado in :activeStates
        """)
    List<LocalTime> findActiveTimesForUserAndDate(@Param("userId") Long userId,
            @Param("date") LocalDate date, @Param("activeStates") Collection<EstadoCita> activeStates);

    Optional<Cita> findByIdAndPaciente_Id(Long id, Long pacienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.id = :id and c.paciente.id = :patientId")
    Optional<Cita> findByIdAndPaciente_IdForUpdate(@Param("id") Long id,
            @Param("patientId") Long pacienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cita c where c.id = :id")
    Optional<Cita> findByIdForUpdate(@Param("id") Long id);

    boolean existsByHorario_Id(Long horarioId);
    boolean existsByEspecialidad_Id(Long especialidadId);

    @Query("""
        select c from Cita c
        where c.paciente.id = :patientId and c.estado in :states
        order by c.horario.fecha desc, c.horario.hora desc
        """)
    List<Cita> findPatientCitas(@Param("patientId") Long patientId,
            @Param("states") Collection<EstadoCita> states);

    @Query("""
        select c from Cita c
        where (:estado is null or c.estado = :estado)
          and (:fecha is null or c.horario.fecha = :fecha)
        order by c.horario.fecha desc, c.horario.hora desc
        """)
    List<Cita> findAgenda(@Param("estado") EstadoCita estado, @Param("fecha") LocalDate fecha);
}
