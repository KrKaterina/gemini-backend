package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.CaseFileResponse;
import com.platform.accident.review.domain.*;
import com.platform.accident.review.exception.*;
import com.platform.accident.review.integration.*;
import com.platform.accident.review.repository.*;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewCaseRepository reviewRepo;
    private final ReviewAuditRepository auditRepo; // Now valid
    private final ReportViewerClient viewerClient;
    private final AiInsightClient aiClient;
    private final ReportLifecycleClient lifecycleClient;

    private static final int LOCK_TIMEOUT_MINUTES = 30;

    /**
     * Entry point from Module 1: Sets up the dashboard entry
     */
    public void initializeReviewQueue(String caseId) {
        if (reviewRepo.findByCaseId(caseId).isEmpty()) {
            reviewRepo.save(ReviewCase.builder()
                    .caseId(caseId)
                    .status(ReviewStatus.PENDING)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    /**
     * Aggregator: Builds the dashboard DTO using type-safe records
     */
//    public CaseFileResponse getConsolidatedCaseFile(String caseId) {
//        var review = reviewRepo.findByCaseId(caseId)
//                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
//
//        var accident = viewerClient.getRawData(caseId)
//                .orElseThrow(() -> new ResourceNotFoundException("Accident missing"));
//
//        // Εδώ παίρνουμε AiAnalysisView Record
//        AiAnalysisView ai = aiClient.getAnalysisResult(caseId)
//                .orElse(new AiAnalysisView("Incomplete", "LOW", java.util.List.of(), "N/A"));
//
//        // Τώρα το 'ai' είναι AiAnalysisView και το 'CaseFileResponse' περιμένει AiAnalysisView.
//        // Το compilation error θα εξαφανιστεί!
//        return new CaseFileResponse(
//                caseId,
//                review.getStatus(),
//                review.getAssignedAgentId(),
//                accident,
//                ai
//        );
//    }
    public CaseFileResponse getConsolidatedCaseFile(String caseId) {
        var review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        var accident = viewerClient.getRawData(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Accident missing"));

        var ai = aiClient.getAnalysisResult(caseId)
                .orElse(new AiAnalysisView("Incomplete", "LOW", List.of(), "N/A"));

        return new CaseFileResponse(
                caseId,
                review.getStatus(),
                review.getAssignedAgentId(),
                accident,
                ai,
                review.getCorrections() // <--- Επιστρέφουμε τις διορθώσεις που κάναμε save στο Verify
        );
    }

    /**
     * Concurrency Safety: Pessimistic Lock Implementation
     */
    public void lockCase(String caseId, String agentId) {
        // ENFORCE AUTHORIZATION: Consulting the Identity Oracle
        if (!identityClient.hasPermission(agentId, "CASE_REVIEW_LOCK")) {
            identityClient.logSecurityEvent(agentId, "AUTH_FAILURE", "Agent tried to lock without permission");
            throw new UnauthorizedReviewException("Insufficient permissions.");
        }

        ReviewCase review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (review.getAssignedAgentId() != null && !review.getAssignedAgentId().equals(agentId)) {
            if (review.getLockedAt().isAfter(Instant.now().minus(30, ChronoUnit.MINUTES))) {
                throw new CaseLockedException("Case locked by agent: " + review.getAssignedAgentId());
            }
        }

        review.setAssignedAgentId(agentId);
        review.setLockedAt(Instant.now());
        review.setStatus(ReviewStatus.IN_PROGRESS);
        reviewRepo.save(review);
    }

    /**
     * Finalizes the case: pushes corrections to the core aggregate.
     */
    public void verifyCase(String caseId, String agentId, Map<String, Object> finalCorrections) {
        // CAPABILITY CHECK
        if (!identityClient.hasPermission(agentId, "CASE_REVIEW_VERIFY")) {
            throw new UnauthorizedReviewException("Cannot verify case.");
        }

        ReviewCase review = reviewRepo.findByCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!agentId.equals(review.getAssignedAgentId())) {
            throw new UnauthorizedReviewException("You do not hold the lock for this case.");
        }

        finalCorrections.put("verifiedByAgent", agentId);
        finalCorrections.put("verificationDate", Instant.now().toString());

        // 1. Persist corrections locally in Review module
        review.setCorrections(finalCorrections);
        review.setStatus(ReviewStatus.VERIFIED);
        reviewRepo.save(review);

        // 2. Synchronize with the Source of Truth (Submission Module)
        lifecycleClient.finalizeReport(caseId, finalCorrections);
        lifecycleClient.updateStatus(caseId, "PROCESSED");

        logAudit(caseId, agentId, "VERIFIED", "Agent " + agentId + " finalized the case.");
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
