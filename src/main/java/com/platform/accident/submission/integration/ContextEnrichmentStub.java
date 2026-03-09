package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;

@Slf4j
@Service
class ContextEnrichmentStub implements ContextEnrichmentClient {
    @Override
    public Map<String, Object> enrichAccidentContext(String caseId, Location location) {
        log.info("[Context Module] Fetching weather/map data for location: {}, {}", location.lat(), location.lng());
        // Simulating enrichment data return
        return Map.of(
                "weather", "Cloudy",
                "temperature", "15C",
                "roadType", "Urban"
        );
    }
}