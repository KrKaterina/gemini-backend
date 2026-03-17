package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Slf4j
@Service
@org.springframework.context.annotation.Profile("dev")
class ContextEnrichmentStub implements ContextEnrichmentClient {

    @Override
    public EnrichmentResponse enrichAccidentContext(String caseId, Location location) {
        log.info("[Context Module] Fetching weather/map data for location: {}, {}", location.lat(), location.lng());

        // Δημιουργούμε dummy data σύμφωνα με το EnrichmentResponse
        String weatherCondition = "Cloudy";
        Double temperature = 15.0;
        String streetName = "Main Street";
        String roadType = "Urban Street";
        String speedLimit = "50km/h";
        boolean contextUnavailable = false;
        Instant processedAt = Instant.now();

        return new EnrichmentResponse(
                weatherCondition,
                temperature,
                streetName,
                roadType,
                speedLimit,
                contextUnavailable,
                processedAt
        );
    }
}