package com.sigesmed.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sigesmed.api.domain.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> { }
