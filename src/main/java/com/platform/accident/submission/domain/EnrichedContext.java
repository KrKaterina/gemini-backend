package com.platform.accident.submission.domain;

import com.platform.accident.submission.integration.EnrichmentResponse;
import lombok.Builder;

/**
 * Replaces Map<String, Object> for contextData with typed values
 */
@Builder
public record EnrichedContext(
        String weatherCondition,
        String weatherDescription,
        Double temperatureCelsius,
        String roadType,
        String neighborhood,
        boolean daylight,
        Integer speedLimit
) {
    public static EnrichedContext fromResponse(EnrichmentResponse res, Integer parsedSpeed) {
        return EnrichedContext.builder()
                .weatherCondition(res.weatherCondition())
                .weatherDescription(com.platform.accident.enrichment.util.WeatherCodeMapper.translate(res.weatherCondition()))
                .temperatureCelsius(res.temperature())
                .roadType(res.roadType())
                .neighborhood(res.streetName())
                .speedLimit(parsedSpeed)
                .daylight(true)
                .build();
    }
}