package com.platform.integration.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public class SecurityContext {
    private static final String ATTR = "USER_CONTEXT";

    public static IdentityContext getRequired(HttpServletRequest request) {
//        return Optional.ofNullable((IdentityContext) request.getAttribute(ATTR))
//                .orElseThrow(() -> new RuntimeException("Unauthorized: No Security Context found"));

        IdentityContext ctx = (IdentityContext) request.getAttribute("USER_CONTEXT");
        if (ctx == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid or missing JWT token");
        }
        return ctx;
    }
}