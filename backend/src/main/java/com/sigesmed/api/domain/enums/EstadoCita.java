package com.sigesmed.api.domain.enums;

import java.util.Set;

public enum EstadoCita {
    RESERVADA,
    CONFIRMADA,
    ATENDIDA,
    CANCELADA,
    NO_ASISTIO;

    public static final Set<EstadoCita> ACTIVAS = Set.of(RESERVADA, CONFIRMADA);
}
