package com.platform.accident.enrichment.service;

import com.platform.accident.enrichment.client.RoadAdapter;
import com.platform.accident.enrichment.client.WeatherAdapter;
import com.platform.accident.enrichment.domain.RoadGeometry;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import com.platform.accident.enrichment.repository.ContextCacheRepository;
import com.platform.accident.enrichment.repository.ExternalContextCache;
import com.platform.accident.submission.domain.Location;
import com.platform.accident.submission.integration.EnrichmentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrichmentOrchestratorTest {

    @Mock private WeatherAdapter weatherAdapter;
    @Mock private RoadAdapter roadAdapter;
    @Mock private ContextCacheRepository cacheRepository;

    @InjectMocks
    private EnrichmentOrchestrator orchestrator;

    @Test
    @DisplayName("Enrich: Should return data from Cache when available (Cache Hit)")
    void enrichAccidentContext_CacheHit() {
        Location loc = new Location(38.0, 23.0, "Athens");
        ExternalContextCache cachedEntry = ExternalContextCache.builder()
                .weatherData(new WeatherMetrics("CLEAR", 25.0))
                .roadData(new RoadGeometry("Main St", "primary", "50"))
                .build();

        when(cacheRepository.findByGeoHashAndReferenceTime(anyString(), any()))
                .thenReturn(Optional.of(cachedEntry));

        EnrichmentResponse response = orchestrator.enrichAccidentContext("CASE123", loc);

        assertThat(response.weatherCondition()).isEqualTo("CLEAR");
        assertThat(response.streetName()).isEqualTo("Main St");
        assertThat(response.contextUnavailable()).isFalse();

        verifyNoInteractions(weatherAdapter, roadAdapter);
    }

    @Test
    @DisplayName("Enrich: Should fetch from APIs and Save to Cache when cache is empty (Cache Miss)")
    void enrichAccidentContext_CacheMiss_Success() {
        Location loc = new Location(38.0, 23.0, "Athens");

        when(cacheRepository.findByGeoHashAndReferenceTime(anyString(), any()))
                .thenReturn(Optional.empty());

        when(weatherAdapter.fetchWeatherAtTimestamp(anyDouble(), anyDouble(), any()))
                .thenReturn(Mono.just(new WeatherMetrics("RAIN", 15.0)));
        when(roadAdapter.fetchRoadMetadata(anyDouble(), anyDouble()))
                .thenReturn(Mono.just(new RoadGeometry("Rural Rd", "secondary", "90")));

        when(cacheRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EnrichmentResponse response = orchestrator.enrichAccidentContext("CASE123", loc);

        assertThat(response.weatherCondition()).isEqualTo("RAIN");
        assertThat(response.speedLimit()).isEqualTo("90");
        assertThat(response.contextUnavailable()).isFalse();

        verify(cacheRepository).save(any(ExternalContextCache.class));
    }

    @Test
    @DisplayName("Enrich: Should return Fallback Response when any API or DB fails")
    void enrichAccidentContext_OnError_ReturnsFallback() {
        Location loc = new Location(38.0, 23.0, "Athens");

        when(cacheRepository.findByGeoHashAndReferenceTime(anyString(), any()))
                .thenThrow(new RuntimeException("DB Down"));

        EnrichmentResponse response = orchestrator.enrichAccidentContext("CASE123", loc);

        assertThat(response.contextUnavailable()).isTrue();
        assertThat(response.weatherCondition()).isEqualTo("UNKNOWN");
        assertThat(response.streetName()).isEqualTo("UNKNOWN");
    }
}