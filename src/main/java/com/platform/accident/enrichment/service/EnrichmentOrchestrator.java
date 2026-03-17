package com.platform.accident.enrichment.service;

import com.platform.accident.enrichment.client.*;
import com.platform.accident.enrichment.domain.*;
import com.platform.accident.enrichment.repository.*;
import com.platform.accident.enrichment.util.GeoHashUtils;
import com.platform.accident.submission.domain.Location;
import com.platform.accident.submission.integration.ContextEnrichmentClient;
import com.platform.accident.submission.integration.EnrichmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class EnrichmentOrchestrator implements ContextEnrichmentClient {

    private final WeatherAdapter weatherAdapter;
    private final RoadAdapter roadAdapter;
    private final ContextCacheRepository cacheRepository;

    @Override
    public EnrichmentResponse enrichAccidentContext(String caseId, Location location) {
        try {
            double lat = location.lat();
            double lng = location.lng();
            Instant now = Instant.now();
            String geoHash = GeoHashUtils.encode(lat, lng);
            Instant hourBucket = now.truncatedTo(ChronoUnit.HOURS);

            // 1. Fetch from Cache or External APIs
            ExternalContextCache entry = cacheRepository.findByGeoHashAndReferenceTime(geoHash, hourBucket)
                    .orElseGet(() -> fetchAndCache(geoHash, hourBucket, lat, lng, now));

            // FIX: Map internal domain to Integration DTO
            return new EnrichmentResponse(
                    entry.getWeatherData().condition(),
                    entry.getWeatherData().temperature(),
                    entry.getRoadData().streetName(),
                    entry.getRoadData().roadType(),
                    entry.getRoadData().speedLimit(),
                    false,
                    now
            );

        } catch (Exception e) {
            log.error("Enrichment Orchestration failed for {}", caseId, e);
            return new EnrichmentResponse("UNKNOWN", 0.0, "UNKNOWN", "N/A", "0", true, Instant.now());
        }
    }

    private ExternalContextCache fetchAndCache(String hash, Instant bucket, double lat, double lng, Instant now) {
        var weatherFuture = weatherAdapter.fetchWeatherAtTimestamp(lat, lng, now).toFuture();
        var roadFuture = roadAdapter.fetchRoadMetadata(lat, lng).toFuture();

        CompletableFuture.allOf(weatherFuture, roadFuture).join();

        try {
            ExternalContextCache entry = ExternalContextCache.builder()
                    .geoHash(hash)
                    .referenceTime(bucket)
                    .weatherData(weatherFuture.get())
                    .roadData(roadFuture.get())
                    .expiresAt(now.plus(1, ChronoUnit.DAYS))
                    .build();
            return cacheRepository.save(entry);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}