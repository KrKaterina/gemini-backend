package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.integration.review.AiAnalysisView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntelligenceReviewAdapterTest {

    @Mock
    private AccidentRepository repository;

    @InjectMocks
    private IntelligenceReviewAdapter adapter;

    @Test
    @DisplayName("AI Result: Should correctly map AI data from report map to View Record")
    void getAnalysisResult_Success() {
        String caseId = "ACC-AI-123";
        Map<String, Object> aiMap = new HashMap<>();
        aiMap.put("summary", "Head-on collision");
        aiMap.put("severityLevel", "HIGH");
        aiMap.put("suggestedNextSteps", List.of("Police intervention"));
        aiMap.put("detailedReasoning", "Evidence based on image frame 1");

        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .aiAnalysis(aiMap)
                .build();

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        Optional<AiAnalysisView> result = adapter.getAnalysisResult(caseId);

        assertThat(result).isPresent();
        AiAnalysisView view = result.get();
        assertThat(view.summary()).isEqualTo("Head-on collision");
        assertThat(view.severityLevel()).isEqualTo("HIGH");
        assertThat(view.suggestedNextSteps()).contains("Police intervention");
        assertThat(view.rawAiOutput()).isEqualTo("Evidence based on image frame 1");
    }

    @Test
    @DisplayName("AI Result: Should use default values when specific keys are missing in the map")
    void getAnalysisResult_DefaultValues() {
        String caseId = "ACC-AI-DEFAULT";
        Map<String, Object> aiMap = new HashMap<>();

        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .aiAnalysis(aiMap)
                .build();

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        Optional<AiAnalysisView> result = adapter.getAnalysisResult(caseId);

        assertThat(result).isPresent();
        AiAnalysisView view = result.get();
        assertThat(view.summary()).isEqualTo("No summary");
        assertThat(view.severityLevel()).isEqualTo("LOW");
        assertThat(view.rawAiOutput()).isEqualTo("No detailed reasoning available");
    }

    @Test
    @DisplayName("AI Result: Should return empty Optional if the accident report has no AI data yet")
    void getAnalysisResult_NoAiDataInReport() {
        String caseId = "ACC-NO-AI";
        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .aiAnalysis(null)
                .build();

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        Optional<AiAnalysisView> result = adapter.getAnalysisResult(caseId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("AI Result: Should return empty Optional if the report caseId is not found")
    void getAnalysisResult_ReportNotFound() {
        String caseId = "NOT-FOUND";
        when(repository.findByCaseId(caseId)).thenReturn(Optional.empty());

        Optional<AiAnalysisView> result = adapter.getAnalysisResult(caseId);

        assertThat(result).isEmpty();
    }
}