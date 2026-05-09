package com.platform.accident.submission.service;

import com.platform.accident.submission.api.dto.AccidentReportRequest;
import com.platform.accident.submission.domain.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccidentMapperTest {

    private final AccidentMapper mapper = new AccidentMapper();

    @Test
    @DisplayName("Map DTO to Input: Should correctly transfer all fields")
    void toInput_ShouldMapAllFields() {
        Instant now = Instant.now();
        Location location = new Location(37.98, 23.72, "Athens, Greece");
        List<String> assets = List.of("asset-1", "asset-2");
        String description = "Minor car accident at the intersection";

        AccidentReportRequest request = new AccidentReportRequest(
                now,
                location,
                description,
                assets
        );

        AccidentReportInput result = mapper.toInput(request);

        assertThat(result).isNotNull();
        assertThat(result.occurrenceTime()).isEqualTo(now);
        assertThat(result.location()).isEqualTo(location);
        assertThat(result.description()).isEqualTo(description);
        assertThat(result.assetIds()).isEqualTo(assets);
        assertThat(result.assetIds()).hasSize(2);
    }

    @Test
    @DisplayName("Map DTO to Input: Should handle empty asset lists")
    void toInput_WithEmptyAssets_ShouldMapCorrectly() {
        AccidentReportRequest request = new AccidentReportRequest(
                Instant.now(),
                new Location(0, 0, "Unknown"),
                "Test",
                List.of() // Κενή λίστα
        );

        AccidentReportInput result = mapper.toInput(request);

        assertThat(result.assetIds()).isEmpty();
    }
}