package com.platform.accident.enrichment.service;

import com.platform.accident.enrichment.repository.ContextCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CacheProviderTest {

    @Mock
    private ContextCacheRepository repository;

    @InjectMocks
    private CacheProvider cacheProvider;

    @Test
    @DisplayName("GeoHash: Should generate a deterministic string from lat/lng coordinates")
    void generateGeoHash_LogicCheck() {
        double lat = 37.9838;
        double lng = 23.7275;

        String expectedHash = "37.923.";

        String result = cacheProvider.generateGeoHash(lat, lng);

        assertThat(result).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Truncation: Should correctly remove minutes/seconds and return an hour-only timestamp")
    void truncateToHour_LogicCheck() {
        Instant input = Instant.parse("2024-05-09T14:55:30.123Z");
        Instant expected = Instant.parse("2024-05-09T14:00:00Z");

        Instant result = cacheProvider.truncateToHour(input);

        assertThat(result).isEqualTo(expected);
        assertThat(result.isAfter(Instant.MIN)).isTrue();
    }
}