package com.sigesmed.api.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sigesmed.api.domain.entity.RecuperacionClave;
import jakarta.persistence.LockModeType;

public interface RecuperacionClaveRepository extends JpaRepository<RecuperacionClave, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r from RecuperacionClave r
        where r.tokenHash = :tokenHash and r.usado = false and r.expiraEn > :now
        """)
    Optional<RecuperacionClave> findValidForUpdate(@Param("tokenHash") String tokenHash,
            @Param("now") OffsetDateTime now);
}
