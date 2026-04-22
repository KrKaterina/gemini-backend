package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.CaseFileResponse;
import com.platform.accident.review.domain.*;
import com.platform.accident.review.exception.*;
import com.platform.accident.review.integration.*;
import com.platform.accident.review.repository.*;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

import com.platform.accident.review.domain.*;
import com.platform.accident.review.repository.ReviewCaseRepository;
import com.platform.accident.review.repository.ReviewAuditRepository; // <--- FIX
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.review.integration.ReportLifecycleClient;
import com.platform.accident.review.exception.CaseLockedException;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import com.platform.integration.review.AiAnalysisView;
import com.platform.accident.review.api.dto.CaseFileResponse;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewCaseRepository reviewRepo;
    private final ReviewAuditRepository auditRepo; // Now valid
    private final ReportViewerClient viewerClient;
    private final AiInsightClient aiClient;
    private final ReportLifecycleClient lifecycleClient;

    private final IdentityClient identityClient;

    private static final int LOCK_TIMEOUT_MINUTES = 30;

    /**
     * Entry point from Module 1: Sets up the dashboard entry
     */
    public void initializeReviewQueue(String caseId) {
        reviewRepo.findByCaseId(caseId).ifPresentOrElse(
                existing -> log.info("Case {} already in queue", caseId),
                () -> reviewRepo.save(ReviewCase.builder()
                        .caseId(caseId)
                        .status(ReviewStatus.PENDING)
                        .createdAt(Instant.now())
                        .build())
        );
    }

    /**
     * Aggregator: Builds the dashboard DTO using type-safe records
     */
    //it works - no authorization
//    public CaseFileResponse getConsolidatedCaseFile(String caseId) {
//        var review = reviewRepo.findByCaseId(caseId)
//                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
//
//        var accident = viewerClient.getRawData(caseId)
//                .orElseThrow(() -> new ResourceNotFoundException("Accident missing"));
//
//        var ai = aiClient.getAnalysisResult(caseId)
//                .orElse(new AiAnalysisView("Incomplete", "LOW", List.of(), "N/A"));
//
//        return new CaseFileResponse(
//                caseId,
//                review.getStatus(),
//                review.getAssignedAgentId(),
//                accident,
//                ai,
//                review.getCorrections() // <--- Επιστρέφουμε τις διορθώσεις που κάναμε save στο Verify
//        );
//    }
    public CaseFileResponse getConsolidatedCaseFile(String caseId, IdentityContext agentCtx) {
        // AUTHORIZATION:
        if (!identityClient.hasPermission(agentCtx.userId(), "ACCIDENT_REPORT_VIEW_ALL")) {
            identityClient.logSecurityEvent(agentCtx.userId(), "AUTH_FAILURE", "Unauthorized view attempt: " + caseId);
            throw new UnauthorizedReviewException("Insufficient Permissions");
        }

        ReviewCase review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        AccidentSnapshotView snap = viewerClient.getRawData(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Accident record missing"));

        AiAnalysisView ai = aiClient.getAnalysisResult(caseId)
                .orElse(new AiAnalysisView("Processing", "LOW", List.of(), "Waiting for results"));

        return new CaseFileResponse(
                caseId,
                review.getStatus(),
                review.getAssignedAgentId(),
                snap,
                ai,
                review.getCorrections()
        );
    }

    /**
     * Concurrency Safety: Pessimistic Lock Implementation
     */
    public void lockCase(String caseId, IdentityContext agentCtx) {
        String agentId = agentCtx.userId();

        if (!identityClient.hasPermission(agentId, "CASE_REVIEW_LOCK")) {
            identityClient.logSecurityEvent(agentId, "AUTH_FAILURE", "Unauthorized lock attempt");
            throw new UnauthorizedReviewException("Insufficient permissions.");
        }

        ReviewCase review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        // Concurrency Logic
        if (review.getAssignedAgentId() != null && !review.getAssignedAgentId().equals(agentId)) {
            if (review.getLockedAt() != null && review.getLockedAt().isAfter(Instant.now().minus(LOCK_TIMEOUT_MINUTES, ChronoUnit.MINUTES))) {
                throw new CaseLockedException("Case currently reviewed by " + review.getAssignedAgentId());
            }
        }

        review.setAssignedAgentId(agentId);
        review.setLockedAt(Instant.now());
        review.setStatus(ReviewStatus.IN_PROGRESS);
        reviewRepo.save(review);

        identityClient.logSecurityEvent(agentId, "CASE_LOCKED", caseId);
    }

    /**
     * Finalizes the case: pushes corrections to the core aggregate.
     */
    @Transactional
    public void verifyCase(String caseId, IdentityContext agentCtx, Map<String, Object> finalCorrections) {
        String agentId = agentCtx.userId();

        if (!identityClient.hasPermission(agentId, "CASE_REVIEW_VERIFY")) {
            throw new UnauthorizedReviewException("Insufficient permissions to verify.");
        }

        ReviewCase review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Review metadata missing"));

        if (!agentId.equals(review.getAssignedAgentId())) {
            throw new UnauthorizedReviewException("Action Forbidden: You do not hold the active lock.");
        }

        finalCorrections.put("verifiedBy", agentId);
        finalCorrections.put("verificationDate", Instant.now().toString());

        review.setCorrections(finalCorrections);
        review.setStatus(ReviewStatus.VERIFIED);
        reviewRepo.save(review);

        lifecycleClient.finalizeReport(caseId, finalCorrections);
        lifecycleClient.updateStatus(caseId, "PROCESSED");

        logAudit(caseId, agentId, "VERIFIED", "Finalized");
        identityClient.logSecurityEvent(agentId, "CASE_VERIFIED_SUCCESS", caseId);
    }


    private void logAudit(String caseId, String agentId, String action, String detail) {
        auditRepo.save(ReviewAuditEntry.builder()
                .caseId(caseId)
                .agentId(agentId)
                .action(action)
                .detail(detail)
                .timestamp(Instant.now())
                .build());
    }
}
