package com.platform.accident.enrichment.repository;

import com.platform.accident.enrichment.domain.RoadGeometry;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalContextCacheTest {

    @Test
    @DisplayName("Should correctly create ExternalContextCache using Builder")
    void testBuilderAndGetters() {
        // Arrange
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expiry = now.plus(24, ChronoUnit.HOURS);

        WeatherMetrics weather = new WeatherMetrics("RAIN", 15.5);
        // Χρήση του σωστού Record Constructor
        RoadGeometry road = new RoadGeometry("Main Street", "primary", "50");

        // Act
        ExternalContextCache cache = ExternalContextCache.builder()
                .id("123")
                .geoHash("sq2m6")
                .referenceTime(now)
                .weatherData(weather)
                .roadData(road)
                .expiresAt(expiry)
                .build();

        // Assert
        assertThat(cache.getId()).isEqualTo("123");
        assertThat(cache.getGeoHash()).isEqualTo("sq2m6");

        // ΔΙΟΡΘΩΣΗ ΕΔΩ: streetName() αντί για name()
        assertThat(cache.getRoadData().streetName()).isEqualTo("Main Street");

        // roadType() αντιστοιχεί στο πεδίο roadType του Record
        assertThat(cache.getRoadData().roadType()).isEqualTo("primary");

        assertThat(cache.getRoadData().speedLimit()).isEqualTo("50");
    }

    @Test
    @DisplayName("Should verify NoArgsConstructor and Setters")
    void testNoArgsConstructorAndSetters() {
        ExternalContextCache cache = new ExternalContextCache();
        cache.setId("id-999");
        cache.setGeoHash("gh123");

        assertThat(cache.getId()).isEqualTo("id-999");
        assertThat(cache.getGeoHash()).isEqualTo("gh123");
    }
}