package com.platform.accident.submission.integration;

import java.time.Instant;

/**
 * Shared DTO to prevent module leakage.
 * Both Submission and Enrichment modules depend on this integration package.
 */
public record EnrichmentResponse(
        String weatherCondition,
        Double temperature,
        String streetName,
        String roadType,
        String speedLimit,
        boolean contextUnavailable,
        Instant processedAt
) {}