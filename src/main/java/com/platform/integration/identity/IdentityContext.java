package com.platform.integration.identity;

import java.util.List;

public record IdentityContext(
        String userId,
        String username,
        List<String> roles,
        String externalReference, // e.g. Mapping to Policy system
        boolean isActive
) {}