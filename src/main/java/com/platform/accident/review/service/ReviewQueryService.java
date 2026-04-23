package com.platform.accident.review.service;

import com.platform.accident.review.api.dto.ReviewQueueItem;
import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.accident.review.repository.ReviewCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
        // Find review records in our module (Lock info and Status)
        List<ReviewCase> cases = (agentIdFilter != null)
                ? reviewRepo.findByAssignedAgentIdAndStatus(agentIdFilter, ReviewStatus.IN_PROGRESS)
                : reviewRepo.findByStatus(statusFilter);

        return cases.stream().map(c -> {
            // Aggregate brief data from ports for the list view
            var submission = submissionClient.getRawData(c.getCaseId()).orElse(null);
            var ai = aiClient.getAnalysisResult(c.getCaseId()).orElse(null);

            return new ReviewQueueItem(
                    c.getCaseId(),
                    submission != null ? submission.reporterName() : "Unknown",
                    ai != null ? ai.severityLevel() : "PROCESSING",
                    c.getStatus().name(),
                    java.time.Duration.between(c.getCreatedAt(), java.time.Instant.now()).toMinutes()
            );
        }).toList();
    }
}