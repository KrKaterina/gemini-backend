package com.platform.accident.review.service;

import com.platform.accident.submission.integration.*;
import com.platform.accident.review.domain.*;
import com.platform.accident.review.repository.CaseReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;


//@Service
//@RequiredArgsConstructor
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final CaseReviewRepository reviewRepository;
    private final UnifiedCaseViewerClient caseViewer;
    private final ReviewCallbackPort callbackPort;
    private final VerificationPolicy verificationPolicy;

    /**
     * Attempts to lock a case for a specific agent.
     */
    public void lockCase(String caseId, String agentId) {
        CaseReview review = reviewRepository.findByCaseId(caseId)
                .orElseGet(() -> CaseReview.builder()
                        .caseId(caseId)
                        .status(ReviewStatus.WAITING)
                        .adjustments(new java.util.ArrayList<>()) // Defensive fallback
                        .auditTrail(new java.util.ArrayList<>())   // Defensive fallback
                        .build());

        if (review.isLocked() && !agentId.equals(review.getLockedBy())) {
            throw new IllegalStateException("Case is currently locked by another agent");
        }

        // Defensive check: handle potentially null lists from existing DB records (Legacy data)
        if (review.getAuditTrail() == null) review.setAuditTrail(new java.util.ArrayList<>());
        if (review.getAdjustments() == null) review.setAdjustments(new java.util.ArrayList<>());

        review.setLockedBy(agentId);
        review.setLockedAt(Instant.now());
        review.setLockExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.getAuditTrail().add(new ReviewAuditEntry(Instant.now(), agentId, "ACQUIRE_LOCK"));

        reviewRepository.save(review);
        log.info("Case {} locked successfully by agent {}", caseId, agentId);

    }

    /**
     * Business Logic: Verification.
     * Checks if agent owns the lock and if business policies are met.
     */
    public void verifyCase(String caseId, String agentId, String finalSeverity, List<Adjustment> adjustments) {
        CaseReview review = reviewRepository.findByCaseId(caseId)
                .orElseThrow(() -> new IllegalArgumentException("No review found for case"));

        if (!agentId.equals(review.getLockedBy())) {
            throw new IllegalStateException("Agent does not own the lock on this case");
        }

        // Enforcement: Is the case data valid enough for insurance processing?
        CaseFileView caseData = caseViewer.getCompleteCaseFile(caseId)
                .orElseThrow(() -> new IllegalStateException("Original case data vanished"));

        verificationPolicy.validateReadyForVerification(caseData);

        // State Transition
        review.setStatus(ReviewStatus.VERIFIED);
        review.getAdjustments().addAll(adjustments);
        review.setLockedBy(null); // Release lock
        review.getAuditTrail().add(new ReviewAuditEntry(Instant.now(), agentId, "VERIFY_CASE"));

        reviewRepository.save(review);

        // Notify Submission module of human final verdict
        callbackPort.markCaseAsVerified(caseId, finalSeverity, agentId);
        log.info("Agent {} successfully verified case {}", agentId, caseId);
    }
}
