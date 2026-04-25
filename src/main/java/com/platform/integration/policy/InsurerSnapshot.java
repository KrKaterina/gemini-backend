package com.platform.integration.policy;

public record InsurerSnapshot(
        String declarationId,
        String providerCode,
        String policyNumber,
        String insurerNotificationEndpoint
) {}