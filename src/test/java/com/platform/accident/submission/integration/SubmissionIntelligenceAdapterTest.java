package com.platform.accident.submission.integration;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.review.service.ReviewService;
import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.domain.EnrichedContext;
import com.platform.accident.submission.domain.Location;
import com.platform.accident.submission.repository.AccidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionIntelligenceAdapterTest {

    @Mock
    private AccidentRepository repository;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private SubmissionIntelligenceAdapter adapter;

    @Test
    @DisplayName("Read: Should map AccidentReport and EnrichedContext correctly to AnalysisSourceData")
    void getSourceDataForAnalysis_Success() {
        String caseId = "ACC-001";
        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .rawDescription("Car collision")
                .location(new Location(38.0, 23.0, "Address"))
                .occurrenceTime(Instant.now())
                .assetIds(List.of("asset-1"))
                .contextData(EnrichedContext.builder()
                        .weatherCondition("CODE_1")
                        .roadType("motorway")
                        .neighborhood("Neo Psychiko")
                        .daylight(true)
                        .build())
                .build();

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        Optional<AnalysisSourceData> result = adapter.getSourceDataForAnalysis(caseId);

        assertThat(result).isPresent();
        AnalysisSourceData data = result.get();
        assertThat(data.rawDescription()).isEqualTo("Car collision");
        assertThat(data.weatherCondition()).isEqualTo("CODE_1");
        assertThat(data.neighborhood()).isEqualTo("Neo Psychiko");
        assertThat(data.isDaylight()).isTrue();
        assertThat(data.assetIds()).hasSize(1);
    }

    @Test
    @DisplayName("Read: Should use UNKNOWN/Defaults when EnrichedContext is missing")
    void getSourceDataForAnalysis_NoContext() {
        AccidentReport report = AccidentReport.builder()
                .caseId("ACC-002")
                .location(new Location(0, 0, ""))
                .contextData(null)
                .assetIds(null)
                .build();

        when(repository.findByCaseId("ACC-002")).thenReturn(Optional.of(report));

        Optional<AnalysisSourceData> result = adapter.getSourceDataForAnalysis("ACC-002");

        assertThat(result).isPresent();
        assertThat(result.get().weatherCondition()).isEqualTo("UNKNOWN");
        assertThat(result.get().assetIds()).isEmpty();
    }

    @Test
    @DisplayName("Write: Should persist AI results and hand over to Review Module")
    void onAnalysisComplete_Success() {
        String caseId = "ACC-003";
        AccidentReport report = new AccidentReport();
        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        AiIntelligenceResult aiResult = new AiIntelligenceResult(
                "Technical Summary", null, "MEDIUM", List.of("Next Step"), "Full reasoning"
        );

        adapter.onAnalysisComplete(caseId, aiResult);

        assertThat(report.getStatus()).isEqualTo(AccidentStatus.PENDING_REVIEW);
        assertThat(report.getAiAnalysis()).isNotNull();
        assertThat(report.getAiAnalysis().get("summary")).isEqualTo("Technical Summary");

        verify(repository).save(report);
        verify(reviewService).initializeReviewQueue(caseId); // Η γέφυρα με το Module 4
    }

    @Test
    @DisplayName("Error Handling: Failure in AI should still move report to review queue for manual check")
    void onAnalysisFailure_ShouldStillTriggerManualReview() {
        String caseId = "ACC-FAIL";
        AccidentReport report = new AccidentReport();
        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        adapter.onAnalysisFailure(caseId, "GEMINI_503");

        assertThat(report.getStatus()).isEqualTo(AccidentStatus.PENDING_REVIEW);
        verify(repository).save(report);
        verify(reviewService).initializeReviewQueue(caseId);
    }
}