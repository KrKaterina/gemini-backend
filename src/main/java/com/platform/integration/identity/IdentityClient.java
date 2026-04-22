package com.platform.integration.identity;

import java.util.Optional;

public interface IdentityClient {
    Optional<IdentityContext> validateToken(String token);
    boolean hasPermission(String userId, String permission);
    void logSecurityEvent(String userId, String action, String details);
}