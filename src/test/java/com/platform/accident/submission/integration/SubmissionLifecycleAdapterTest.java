package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.repository.AccidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionLifecycleAdapterTest {

    @Mock
    private AccidentRepository repository;

    @InjectMocks
    private SubmissionLifecycleAdapter adapter;

    @Test
    @DisplayName("Finalize: Should merge data when report already has AI analysis results")
    void finalizeReport_MergeWithExistingAiAnalysis() {
        String caseId = "ACC-100";
        Map<String, Object> existingAi = new HashMap<>();
        existingAi.put("summary", "AI thought this");

        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .aiAnalysis(existingAi)
                .build();

        Map<String, Object> agentData = Map.of("verifiedBy", "agent-1", "correctedSummary", "Agent correction");

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        adapter.finalizeReport(caseId, agentData);

        assertThat(report.getAiAnalysis()).containsKeys("summary", "verifiedBy", "correctedSummary");
        verify(repository).save(report);
    }

    @Test
    @DisplayName("Finalize: Should initialize map when report has null AI analysis")
    void finalizeReport_WithNullAiAnalysis() {
        String caseId = "ACC-200";
        AccidentReport report = AccidentReport.builder().caseId(caseId).aiAnalysis(null).build();
        Map<String, Object> finalData = Map.of("status", "completed");

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        adapter.finalizeReport(caseId, finalData);

        assertThat(report.getAiAnalysis()).isNotNull();
        assertThat(report.getAiAnalysis().get("status")).isEqualTo("completed");
        verify(repository).save(report);
    }

    @Test
    @DisplayName("Status: Should successfully update report status for valid status strings")
    void updateStatus_Success() {
        String caseId = "ACC-300";
        AccidentReport report = new AccidentReport();
        report.setStatus(AccidentStatus.RECEIVED);

        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        adapter.updateStatus(caseId, "PROCESSED");

        assertThat(report.getStatus()).isEqualTo(AccidentStatus.PROCESSED);
        verify(repository).save(report);
    }

    @Test
    @DisplayName("Status: Should catch Exception and skip save for invalid status strings")
    void updateStatus_InvalidStatusString() {
        String caseId = "ACC-400";
        AccidentReport report = new AccidentReport();
        when(repository.findByCaseId(caseId)).thenReturn(Optional.of(report));

        adapter.updateStatus(caseId, "INVALID_STATE_XYZ");

        verify(repository, never()).save(any(AccidentReport.class));
    }

    @Test
    @DisplayName("Logic Check: Methods should do nothing if report is not found")
    void finalizeAndStatus_NotFound_DoNothing() {
        when(repository.findByCaseId("MISSING")).thenReturn(Optional.empty());

        adapter.finalizeReport("MISSING", new HashMap<>());
        adapter.updateStatus("MISSING", "PROCESSED");

        verify(repository, never()).save(any());
    }
}