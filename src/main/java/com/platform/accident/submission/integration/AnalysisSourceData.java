package com.platform.accident.submission.integration;

import java.time.Instant;
import java.util.List;

/**
 * Data needed from the Submission module to perform analysis.
 */
public record AnalysisSourceData(
        String rawDescription, // Υποχρεωτικό
        String weatherCondition,
        String roadType,
        String neighborhood,    // Previously "Ghost"
        boolean isDaylight,
        double lat,
        double lng,
        Instant occurrenceTime,
        String caseId,
        List<String> assetIds   // Required for AI Vision
) {}

