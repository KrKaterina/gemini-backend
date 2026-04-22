package com.platform.integration.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public class SecurityContext {
    private static final String ATTR = "USER_CONTEXT";

    public static IdentityContext getRequired(HttpServletRequest request) {
        return Optional.ofNullable((IdentityContext) request.getAttribute(ATTR))
                .orElseThrow(() -> new RuntimeException("Unauthorized: No Security Context found"));
    }
}