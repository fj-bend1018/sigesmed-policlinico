package com.sigesmed.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class SecurityConfigurationTest {
    @Test
    void applicationClockUsesLimaTimezone() {
        assertEquals(ZoneId.of("America/Lima"), new SecurityConfiguration().applicationClock().getZone());
    }
}
