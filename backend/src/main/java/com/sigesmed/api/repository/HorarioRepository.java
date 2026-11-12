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

import com.sigesmed.api.domain.entity.Horario;
import com.sigesmed.api.domain.enums.EstadoHorario;
import com.sigesmed.api.domain.enums.EstadoCita;
import jakarta.persistence.LockModeType;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    boolean existsByDoctor_IdAndFechaAndHora(Long doctorId, LocalDate fecha, LocalTime hora);
    boolean existsByDoctor_IdAndFechaAndHoraAndIdNot(Long doctorId, LocalDate fecha, LocalTime hora, Long id);
    boolean existsByServicioComplementario_IdAndFechaAndHora(Long servicioId, LocalDate fecha, LocalTime hora);
    boolean existsByServicioComplementario_IdAndFechaAndHoraAndIdNot(Long servicioId, LocalDate fecha, LocalTime hora, Long id);
    List<Horario> findByDoctor_IdAndFechaBetweenOrderByFechaAscHoraAsc(Long doctorId, LocalDate desde, LocalDate hasta);
    List<Horario> findByServicioComplementario_IdAndFechaBetweenOrderByFechaAscHoraAsc(Long servicioId, LocalDate desde, LocalDate hasta);
    List<Horario> findAllByFechaBetweenOrderByFechaAscHoraAsc(LocalDate desde, LocalDate hasta);
    @Query("""
        select h from Horario h
        where h.doctor.id = :doctorId and h.fecha = :date
          and h.disponible = true and h.estado = :availableState
          and not exists (
              select c.id from Cita c
              where c.horario.id = h.id and c.estado in :activeStates
          )
        order by h.hora
        """)
    List<Horario> findAvailableByDoctorAndDate(@Param("doctorId") Long doctorId,
            @Param("date") LocalDate date, @Param("availableState") EstadoHorario availableState,
            @Param("activeStates") Collection<EstadoCita> activeStates);

    @Query("""
        select h from Horario h
        where h.servicioComplementario.id = :serviceId and h.fecha = :date
          and h.disponible = true and h.estado = :availableState
          and not exists (
              select c.id from Cita c
              where c.horario.id = h.id and c.estado in :activeStates
          )
        order by h.hora
        """)
    List<Horario> findAvailableByServiceAndDate(@Param("serviceId") Long serviceId,
            @Param("date") LocalDate date, @Param("availableState") EstadoHorario availableState,
            @Param("activeStates") Collection<EstadoCita> activeStates);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Horario h where h.id = :id")
    Optional<Horario> findByIdForUpdate(@Param("id") Long id);
}
