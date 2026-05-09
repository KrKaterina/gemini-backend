package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.CaseFileResponse;
import com.platform.accident.review.api.dto.ConsolidatedCaseFile;
import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.exception.CaseLockedException;
import com.platform.accident.review.exception.ResourceNotFoundException;
import com.platform.accident.review.exception.UnauthorizedReviewException;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.review.integration.ReportLifecycleClient;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.accident.review.repository.ReviewAuditRepository;
import com.platform.accident.review.repository.ReviewCaseRepository;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.media.MediaAssetClient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewCaseRepository reviewRepo;
    @Mock private ReviewAuditRepository auditRepo;
    @Mock private ReportViewerClient viewerClient;
    @Mock private AiInsightClient aiClient;
    @Mock private ReportLifecycleClient lifecycleClient;
    @Mock private IdentityClient identityClient;
    @Mock private MediaAssetClient mediaClient;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("Initialize: Should create new review record if caseId is new")
    void initializeReviewQueue_NewCase_SavesRecord() {
        String caseId = "ACC-NEW";
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.empty());

        reviewService.initializeReviewQueue(caseId);

        verify(reviewRepo).save(argThat(rc ->
                rc.getCaseId().equals(caseId) && rc.getStatus() == ReviewStatus.PENDING
        ));
    }

    @Test
    @DisplayName("Lock: Should update status to IN_PROGRESS and set locked timestamp")
    void lockCase_Success() {
        String caseId = "ACC-1";
        IdentityContext agent = new IdentityContext("agent-1", "A", List.of(), List.of(), "R", true);
        ReviewCase review = ReviewCase.builder().caseId(caseId).status(ReviewStatus.PENDING).build();

        when(identityClient.hasPermission("agent-1", "CASE_REVIEW_LOCK")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(review));

        reviewService.lockCase(caseId, agent);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.IN_PROGRESS);
        assertThat(review.getAssignedAgentId()).isEqualTo("agent-1");
        assertThat(review.getLockedAt()).isNotNull();
        verify(reviewRepo).save(review);
    }

    @Test
    @DisplayName("Dashboard: Should aggregate data from AI, Viewer and Media ports")
    void getFullDashboardView_Success() {
        String caseId = "ACC-1";
        IdentityContext context = new IdentityContext("agent-1", "user", List.of(), List.of(), "REF", true);

        ReviewCase review = ReviewCase.builder().caseId(caseId).status(ReviewStatus.IN_PROGRESS).build();
        AccidentSnapshotView snap = mock(AccidentSnapshotView.class);
        AiAnalysisView ai = new AiAnalysisView("COMPLETED", "HIGH", List.of(), "OK");

        when(identityClient.hasPermission("agent-1", "ACCIDENT_REPORT_VIEW_ALL")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(review));
        when(viewerClient.getRawData(caseId)).thenReturn(Optional.of(snap));
        when(aiClient.getAnalysisResult(caseId)).thenReturn(Optional.of(ai));
        when(mediaClient.getAssetsByCase(caseId)).thenReturn(List.of());

        ConsolidatedCaseFile result = reviewService.getFullDashboardView(caseId, context);

        assertThat(result).isNotNull();
        assertThat(result.caseId()).isEqualTo(caseId);
        assertThat(result.aiAnalysis()).isEqualTo(ai);
        verify(mediaClient).getAssetsByCase(caseId);
    }

    @Test
    @DisplayName("Verify: Should call LifecycleClient to finalize report in Module 1")
    void verifyCase_FullFlow() {
        String caseId = "ACC-1";
        IdentityContext agent = new IdentityContext("agent-1", "A", List.of(), List.of(), "R", true);
        ReviewCase review = ReviewCase.builder().caseId(caseId).assignedAgentId("agent-1").build();

        when(identityClient.hasPermission("agent-1", "CASE_REVIEW_VERIFY")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(review));

        reviewService.verifyCase(caseId, agent, new HashMap<>());

        verify(lifecycleClient).finalizeReport(eq(caseId), anyMap());
        verify(lifecycleClient).updateStatus(caseId, "PROCESSED");
        verify(auditRepo).save(any()); // Ελέγχει ότι γράφτηκε το Audit Trail
    }

    @Test
    @DisplayName("Security: Should throw UnauthorizedException if agent lacks view permission")
    void getFullDashboardView_NoPermission_ThrowsException() {
        IdentityContext agent = new IdentityContext("spy", "A", List.of(), List.of(), "R", true);
        when(identityClient.hasPermission("spy", "ACCIDENT_REPORT_VIEW_ALL")).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getFullDashboardView("C1", agent))
                .isInstanceOf(UnauthorizedException.class);

        verify(identityClient).logSecurityEvent(eq("spy"), eq("AUTH_FAILURE"), anyString());
    }

    @Test
    @DisplayName("Concurrency: Fail lock when another agent holds it (within 30 mins)")
    void lockCase_Fail_CurrentlyActive() {
        String caseId = "C1";
        IdentityContext newAgent = new IdentityContext("agent-new", "A", List.of(), List.of(), "R", true);
        ReviewCase activeReview = ReviewCase.builder()
                .caseId(caseId)
                .assignedAgentId("agent-active")
                .lockedAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        when(identityClient.hasPermission("agent-new", "CASE_REVIEW_LOCK")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(activeReview));

        assertThatThrownBy(() -> reviewService.lockCase(caseId, newAgent))
                .isInstanceOf(CaseLockedException.class);
    }
}