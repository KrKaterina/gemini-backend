package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.domain.EnrichedContext;
import com.platform.accident.submission.domain.Location;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccidentSnapshotMapperTest {

    private final AccidentSnapshotMapper mapper = new AccidentSnapshotMapper();

    @Test
    @DisplayName("Map View: Should apply motorway speed heuristic and round temperature")
    void mapToView_HeuristicMotorwayAndRounding() {
        AccidentReport report = AccidentReport.builder()
                .caseId("ACC-1")
                .location(new Location(37.0, 23.0, "Attiki Odos"))
                .contextData(EnrichedContext.builder()
                        .roadType("motorway")
                        .speedLimit(0)
                        .temperatureCelsius(25.555)
                        .weatherCondition("CODE_0")
                        .build())
                .occurrenceTime(Instant.now())
                .build();

        AccidentSnapshotView view = mapper.mapToView(report);

        assertThat(view.speedLimit()).isEqualTo(120);
        assertThat(view.temperature()).isEqualTo(25.6);
        assertThat(view.weather()).isEqualTo("Clear sky");
        assertThat(view.address()).isEqualTo("Attiki Odos");
    }

    @Test
    @DisplayName("Map View: Should apply residential speed heuristic (30km/h)")
    void mapToView_HeuristicResidential() {
        AccidentReport report = AccidentReport.builder()
                .contextData(EnrichedContext.builder()
                        .roadType("residential")
                        .speedLimit(null)
                        .build())
                .build();

        AccidentSnapshotView view = mapper.mapToView(report);

        assertThat(view.speedLimit()).isEqualTo(30);
    }

    @Test
    @DisplayName("Map View: Should handle completely null context (Defaults to residential/30km/h)")
    void mapToView_NullSafeguards() {
        AccidentReport report = new AccidentReport();

        AccidentSnapshotView view = mapper.mapToView(report);

        assertThat(view.speedLimit()).isEqualTo(30);
        assertThat(view.weather()).isEqualTo("N/A");
        assertThat(view.address()).isEqualTo("Unknown Location");
        assertThat(view.lat()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Map AI: Should extract values from AI Map successfully")
    void mapAiInsights_Success() {
        Map<String, Object> aiMap = new HashMap<>();
        aiMap.put("summary", "Crash at 50km/h");
        aiMap.put("severityLevel", "MEDIUM");
        aiMap.put("suggestedNextSteps", List.of("Verify ID"));
        aiMap.put("detailedReasoning", "Visible bumper damage");

        AccidentReport report = AccidentReport.builder().aiAnalysis(aiMap).build();

        AiAnalysisView view = mapper.mapAiInsights(report);

        assertThat(view.summary()).isEqualTo("Crash at 50km/h");
        assertThat(view.severityLevel()).isEqualTo("MEDIUM");
        assertThat(view.suggestedNextSteps()).contains("Verify ID");
    }

    @Test
    @DisplayName("Map AI: Should return empty view if analysis map is null")
    void mapAiInsights_NullAnalysis() {
        AccidentReport report = new AccidentReport();

        AiAnalysisView view = mapper.mapAiInsights(report);

        assertThat(view.severityLevel()).isEqualTo("N/A");
    }
}