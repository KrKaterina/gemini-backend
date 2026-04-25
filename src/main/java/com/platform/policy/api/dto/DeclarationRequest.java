package com.platform.policy.api.dto;

import java.time.Instant;

public record DeclarationRequest(
        String providerCode,
        String policyNumber,
        Instant validFrom,
        Instant expiryDate,
        String proofAssetId
) {}