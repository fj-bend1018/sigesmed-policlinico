package com.sigesmed.api.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevRecoveryLinkNotifier implements RecoveryLinkNotifier {
    private static final Logger log = LoggerFactory.getLogger(DevRecoveryLinkNotifier.class);
    private final String frontendOrigin;

    public DevRecoveryLinkNotifier(@Value("${sigesmed.frontend-origin:http://localhost:4200}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    @Override
    public void notify(String email, String rawToken) {
        log.info("Enlace de recuperación para {}: {}/restablecer?token={}", email, frontendOrigin, rawToken);
    }
}
