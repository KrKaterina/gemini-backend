package com.platform.integration.identity;

import java.util.List;

/**
 * The ONLY allowed object for identity propagation.
 * Shared across all module boundaries.
 */
public record IdentityContext(
        String userId,
        String username,
        List<String> roles,
        List<String> permissions,
        String externalReference,
        boolean isAuthenticated
) {}