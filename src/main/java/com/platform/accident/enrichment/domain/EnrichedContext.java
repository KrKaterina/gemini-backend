package com.platform.accident.enrichment.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EnrichedContext {
    private final WeatherMetrics weather;
    private final RoadGeometry road;
    private final boolean contextUnavailable;
    private final Instant processedAt;

    // Null Object Pattern
    public static EnrichedContext empty() {
        return EnrichedContext.builder()
                .contextUnavailable(true)
                .processedAt(Instant.now())
                .build();
    }
}