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
        double lat,
        double lng,
        Instant occurrenceTime,
        String caseId
) {}

