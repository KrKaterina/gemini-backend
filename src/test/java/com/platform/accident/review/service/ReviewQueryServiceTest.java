package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.ReviewQueueItem;
import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.accident.review.repository.ReviewCaseRepository;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @Mock private ReviewCaseRepository reviewRepo;
    @Mock private ReportViewerClient submissionClient;
    @Mock private AiInsightClient aiClient;

    @InjectMocks
    private ReviewQueryService queryService;

    @Test
    @DisplayName("Queue: Should filter and return only PENDING case IDs")
    void getPendingReviewQueue_Success() {
        // Arrange
        ReviewCase c1 = ReviewCase.builder().caseId("CASE-1").status(ReviewStatus.PENDING).build();
        ReviewCase c2 = ReviewCase.builder().caseId("CASE-2").status(ReviewStatus.IN_PROGRESS).build();

        when(reviewRepo.findAll()).thenReturn(List.of(c1, c2));

        // Act
        List<String> result = queryService.getPendingReviewQueue();

        // Assert
        assertThat(result).hasSize(1).containsExactly("CASE-1");
    }

    @Test
    @DisplayName("Dashboard: Should correctly map Domain objects to ReviewQueueItem DTOs with aggregated data")
    void getAgentDashboard_MappingVerification() {
        // Arrange
        String caseId = "ACC-001";
        ReviewCase reviewCase = ReviewCase.builder()
                .caseId(caseId)
                .assignedAgentId("agent-1")
                .status(ReviewStatus.IN_PROGRESS)
                .createdAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();

        // Mock Submission Data (Module 1)
        AccidentSnapshotView snapshot = mock(AccidentSnapshotView.class);
        when(snapshot.reporterName()).thenReturn("Kostas Papadopoulos");
        when(submissionClient.getRawData(caseId)).thenReturn(Optional.of(snapshot));

        // Mock AI Insight (Module 3)
        AiAnalysisView aiView = mock(AiAnalysisView.class);
        when(aiView.severityLevel()).thenReturn("HIGH");
        when(aiClient.getAnalysisResult(caseId)).thenReturn(Optional.of(aiView));

        when(reviewRepo.findByStatus(ReviewStatus.IN_PROGRESS)).thenReturn(List.of(reviewCase));

        // Act
        List<ReviewQueueItem> dashboard = queryService.getAgentDashboard(ReviewStatus.IN_PROGRESS, null);

        // Assert
        assertThat(dashboard).hasSize(1);
        ReviewQueueItem item = dashboard.get(0);

        assertThat(item.caseId()).isEqualTo(caseId);
        assertThat(item.reporterName()).isEqualTo("Kostas Papadopoulos");
        assertThat(item.severity()).isEqualTo("HIGH");
        assertThat(item.status()).isEqualTo("IN_PROGRESS");
        assertThat(item.minutesOpen()).isGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("Audit Feed: Should return verified reports for history view")
    void getVerfiedReports_Success() {
        // Arrange
        ReviewCase verifiedCase = ReviewCase.builder()
                .caseId("V-1")
                .status(ReviewStatus.VERIFIED)
                .build();

        when(reviewRepo.findByStatusIn(anyList())).thenReturn(List.of(verifiedCase));
        // We use empty() to check fallback logic in mapping
        when(submissionClient.getRawData("V-1")).thenReturn(Optional.empty());
        when(aiClient.getAnalysisResult("V-1")).thenReturn(Optional.empty());

        // Act
        List<ReviewQueueItem> result = queryService.getVerfiedReports("admin");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reporterName()).isEqualTo("Unknown"); // Check Fallback
        assertThat(result.get(0).severity()).isEqualTo("PROCESSING"); // Check Fallback
        verify(reviewRepo).findByStatusIn(anyList());
    }
}