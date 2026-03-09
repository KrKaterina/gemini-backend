package com.platform.accident.submission.domain;

import lombok.Builder;

/**
 * Replaces Map<String, Object> for contextData with typed values
 */
@Builder
public record EnrichedContext(
        String weatherCondition,
        Double temperatureCelsius,
        String roadType,
        String neighborhood,
        boolean daylight
) {}