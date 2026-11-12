package com.sigesmed.api.service.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class DisabledRecoveryLinkNotifier implements RecoveryLinkNotifier {
    @Override public void notify(String email, String rawToken) { }
}
