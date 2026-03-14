package com.platform.accident.enrichment.service;

import com.platform.accident.enrichment.domain.RoadGeometry;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import com.platform.accident.enrichment.util.GeoHashUtils;
import com.platform.accident.submission.domain.Location; // External module import
import com.platform.accident.submission.integration.ContextEnrichmentClient;
import com.platform.accident.enrichment.client.RoadAdapter;
import com.platform.accident.enrichment.client.WeatherAdapter;
import com.platform.accident.enrichment.domain.EnrichedContext;
import com.platform.accident.enrichment.repository.ContextCacheRepository;
import com.platform.accident.enrichment.repository.ExternalContextCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("prod")
public class EnrichmentOrchestrator implements ContextEnrichmentClient {

    private final WeatherAdapter weatherAdapter;
    private final RoadAdapter roadAdapter;
    private final ContextCacheRepository cacheRepository;

    @Override
    public Map<String, Object> enrichAccidentContext(String caseId, Location location) {
        try {
            double lat = location.lat();
            double lng = location.lng();
            Instant now = Instant.now();
            String geoHash = GeoHashUtils.encode(lat, lng);
            Instant hourBucket = now.truncatedTo(ChronoUnit.HOURS);

            // 1. Check Cache
            var cached = cacheRepository.findByGeoHashAndReferenceTime(geoHash, hourBucket);
            if (cached.isPresent()) {
                log.info("Cache hit for case: {}", caseId);
                return mapToMap(cached.get());
            }

            // 2. Parallel Reactive Fetching
            var weatherFuture = weatherAdapter.fetchHistoricalWeather(lat, lng, now).toFuture();
            var roadFuture = roadAdapter.fetchRoadMetadata(lat, lng).toFuture();
            CompletableFuture.allOf(weatherFuture, roadFuture).join();

            WeatherMetrics weather = weatherFuture.get();
            RoadGeometry road = roadFuture.get();

            if (weather == null) weather = new WeatherMetrics("UNKNOWN", 0.0);
            if (road == null) road = new RoadGeometry("Unknown Street", "N/A", "0");

            // 3. Cache persist
            ExternalContextCache entry = ExternalContextCache.builder()
                    .geoHash(geoHash)
                    .referenceTime(hourBucket)
                    .weatherData(weather)
                    .roadData(road)
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .build();
            cacheRepository.save(entry);

            // 4. Return as Map
            return mapToMap(entry);

        } catch (Exception e) {
            log.error("Enrichment failed for case {}: {}", caseId, e.getMessage(), e);
            return Map.of(
                    "weather", null,
                    "road", null,
                    "contextUnavailable", true,
                    "processedAt", Instant.now()
            );
        }
    }

    private Map<String, Object> mapToMap(ExternalContextCache cache) {
        return Map.of(
                "weather", cache.getWeatherData(),
                "road", cache.getRoadData(),
                "contextUnavailable", false,
                "processedAt", Instant.now()
        );
    }
}