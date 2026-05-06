package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.ReviewQueueItem;
import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.accident.review.repository.ReviewCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewCaseRepository reviewRepo;
    private final ReportViewerClient submissionClient;
    private final AiInsightClient aiClient;

    /**
     * Discovery endpoint for the Agent Dashboard.
     */
    public List<String> getPendingReviewQueue() {
        return reviewRepo.findAll().stream()
                .filter(c -> c.getStatus() == ReviewStatus.PENDING)
                .map(com.platform.accident.review.domain.ReviewCase::getCaseId)
                .toList();
    }

    public List<ReviewQueueItem> getAgentDashboard(ReviewStatus statusFilter, String agentIdFilter) {
        List<ReviewCase> cases = (agentIdFilter != null)
                ? reviewRepo.findByAssignedAgentIdAndStatus(agentIdFilter, ReviewStatus.IN_PROGRESS)
                : reviewRepo.findByStatus(statusFilter);

        return cases.stream().map(this::mapToQueueItem).toList();
    }

    /**
     * Fulfills the "Audit History" requirement:
     * - Statuses: IN_PROGRESS (others), VERIFIED (everyone).
     */
    public List<ReviewQueueItem> getVerfiedReports(String currentAgentId) {
        List<ReviewStatus> completedStatuses = List.of(ReviewStatus.VERIFIED);
        List<ReviewCase> cases = reviewRepo.findByStatusIn(completedStatuses);

        return cases.stream()
                .map(this::mapToQueueItem)
                .toList();
    }

    private ReviewQueueItem mapToQueueItem(ReviewCase c) {
        var submission = submissionClient.getRawData(c.getCaseId()).orElse(null);
        var ai = aiClient.getAnalysisResult(c.getCaseId()).orElse(null);

        int minsOpen = 0;
        if (c.getCreatedAt() != null) {
            minsOpen = (int) Duration.between(c.getCreatedAt(), Instant.now()).toMinutes();
        }

        return new ReviewQueueItem(
                c.getCaseId(),                                          // 1. caseId
                submission != null ? submission.reporterName() : "Unknown", // 2. reporterName
                ai != null ? ai.severityLevel() : "PROCESSING",          // 3. severity
                c.getStatus().name(),                                   // 4. status
                minsOpen,                                               // 5. minutesOpen (long/int)
                c.getAssignedAgentId()                                  // 6. assignedAgentId
        );
    }
}