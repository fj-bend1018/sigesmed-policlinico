package com.sigesmed.api.service.auth;

public interface RecoveryLinkNotifier {
    void notify(String email, String rawToken);
}
