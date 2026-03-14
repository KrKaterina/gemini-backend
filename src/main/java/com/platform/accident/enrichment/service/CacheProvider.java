package com.platform.accident.enrichment.service;

import com.platform.accident.enrichment.repository.ContextCacheRepository;
import com.platform.accident.enrichment.repository.ExternalContextCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class CacheProvider {
    private final ContextCacheRepository repository;

    public String generateGeoHash(double lat, double lng) {
        // Implementation of 7-char GeoHash logic (using a substring or logic helper)
        return String.valueOf(lat).substring(0, 4) + String.valueOf(lng).substring(0, 3);
    }

    public Instant truncateToHour(Instant timestamp) {
        return timestamp.truncatedTo(ChronoUnit.HOURS);
    }
}