package com.platform.accident.submission.integration;

import java.time.Instant;

/**
 * Data needed from the Submission module to perform analysis.
 */
public record AnalysisSourceData(
        String rawDescription,
        String weatherCondition,
        String roadType,
        double lat,
        double lng,
        Instant occurrenceTime
) {}