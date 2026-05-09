package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.CaseFileResponse;
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
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.media.MediaAssetClient;
import com.platform.integration.review.AccidentSnapshotView;
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
    @DisplayName("Lock: Should lock case successfully when PENDING and user has permission")
    void lockCase_Success() {
        String caseId = "ACC-1";
        IdentityContext agent = new IdentityContext("agent-1", "A", List.of(), List.of(), "R", true);
        ReviewCase review = ReviewCase.builder().caseId(caseId).status(ReviewStatus.PENDING).build();

        when(identityClient.hasPermission("agent-1", "CASE_REVIEW_LOCK")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(review));

        reviewService.lockCase(caseId, agent);

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.IN_PROGRESS);
        assertThat(review.getAssignedAgentId()).isEqualTo("agent-1");
        verify(reviewRepo).save(review);
        verify(identityClient).logSecurityEvent(eq("agent-1"), eq("CASE_LOCKED"), eq(caseId));
    }

    @Test
    @DisplayName("Lock: Should fail when case is already locked by another agent within timeout")
    void lockCase_Fail_AlreadyLocked() {
        String caseId = "ACC-1";
        IdentityContext agentA = new IdentityContext("agent-A", "A", List.of(), List.of(), "R", true);

        ReviewCase alreadyLocked = ReviewCase.builder()
                .caseId(caseId)
                .assignedAgentId("agent-B")
                .lockedAt(Instant.now().minus(5, ChronoUnit.MINUTES)) // Κλειδώθηκε πριν 5 λεπτά
                .status(ReviewStatus.IN_PROGRESS)
                .build();

        when(identityClient.hasPermission("agent-A", "CASE_REVIEW_LOCK")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(alreadyLocked));

        assertThatThrownBy(() -> reviewService.lockCase(caseId, agentA))
                .isInstanceOf(CaseLockedException.class);
    }

    @Test
    @DisplayName("Lock: Should allow re-lock if the previous lock has expired (>30 mins)")
    void lockCase_Success_WhenPreviousLockExpired() {
        String caseId = "ACC-1";
        IdentityContext agentA = new IdentityContext("agent-A", "A", List.of(), List.of(), "R", true);

        ReviewCase expiredLock = ReviewCase.builder()
                .caseId(caseId)
                .assignedAgentId("agent-B")
                .lockedAt(Instant.now().minus(40, ChronoUnit.MINUTES)) // Έληξε (30' όριο)
                .status(ReviewStatus.IN_PROGRESS)
                .build();

        when(identityClient.hasPermission("agent-A", "CASE_REVIEW_LOCK")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(expiredLock));

        reviewService.lockCase(caseId, agentA);

        assertThat(expiredLock.getAssignedAgentId()).isEqualTo("agent-A");
        verify(reviewRepo).save(expiredLock);
    }

    @Test
    @DisplayName("Verify: Should successfully finalize report when agent holds the lock")
    void verifyCase_Success() {
        String caseId = "ACC-1";
        IdentityContext agent = new IdentityContext("agent-1", "A", List.of(), List.of(), "R", true);
        ReviewCase review = ReviewCase.builder().caseId(caseId).assignedAgentId("agent-1").build();

        when(identityClient.hasPermission("agent-1", "CASE_REVIEW_VERIFY")).thenReturn(true);
        when(reviewRepo.findByCaseId(caseId)).thenReturn(Optional.of(review));

        reviewService.verifyCase(caseId, agent, new HashMap<>());

        assertThat(review.getStatus()).isEqualTo(ReviewStatus.VERIFIED);
        verify(lifecycleClient).finalizeReport(eq(caseId), anyMap());
        verify(lifecycleClient).updateStatus(caseId, "PROCESSED");
        verify(auditRepo).save(any());
    }

    @Test
    @DisplayName("Verify: Should fail when agent does not hold the active lock")
    void verifyCase_Fail_NotHolder() {
        IdentityContext agent = new IdentityContext("agent-1", "A", List.of(), List.of(), "R", true);
        ReviewCase review = ReviewCase.builder().caseId("C1").assignedAgentId("different-agent").build();

        when(identityClient.hasPermission("agent-1", "CASE_REVIEW_VERIFY")).thenReturn(true);
        when(reviewRepo.findByCaseId("C1")).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.verifyCase("C1", agent, new HashMap<>()))
                .isInstanceOf(UnauthorizedReviewException.class)
                .hasMessageContaining("Action Forbidden");
    }

    @Test
    @DisplayName("GetFile: Fail when accident snapshot is missing from submission module")
    void getConsolidatedCaseFile_MissingData_ThrowsException() {
        IdentityContext agent = new IdentityContext("a1", "A", List.of(), List.of(), "R", true);
        when(identityClient.hasPermission(anyString(), anyString())).thenReturn(true);
        when(reviewRepo.findByCaseId(anyString())).thenReturn(Optional.of(new ReviewCase()));
        when(viewerClient.getRawData(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getConsolidatedCaseFile("CASE-X", agent))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Accident record missing");
    }
}