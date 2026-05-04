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

//    public List<ReviewQueueItem> getAgentDashboard(ReviewStatus statusFilter, String agentIdFilter) {
//        // Find review records in our module (Lock info and Status)
//        List<ReviewCase> cases = (agentIdFilter != null)
//                ? reviewRepo.findByAssignedAgentIdAndStatus(agentIdFilter, ReviewStatus.IN_PROGRESS)
//                : reviewRepo.findByStatus(statusFilter);
//
//        return cases.stream().map(c -> {
//            // Aggregate brief data from ports for the list view
//            var submission = submissionClient.getRawData(c.getCaseId()).orElse(null);
//            var ai = aiClient.getAnalysisResult(c.getCaseId()).orElse(null);
//
//            return new ReviewQueueItem(
//                    c.getCaseId(),
//                    submission != null ? submission.reporterName() : "Unknown",
//                    ai != null ? ai.severityLevel() : "PROCESSING",
//                    c.getStatus().name(),
//                    java.time.Duration.between(c.getCreatedAt(), java.time.Instant.now()).toMinutes()
//            );
//        }).toList();
//    }
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
    public List<ReviewQueueItem> getTotalVisibilityFeed(String currentAgentId) {
        List<ReviewStatus> statuses = List.of(ReviewStatus.IN_PROGRESS, ReviewStatus.VERIFIED);
        List<ReviewCase> cases = reviewRepo.findByStatusIn(statuses);

        return cases.stream()
                // Δείχνουμε τα πάντα εκτός από αυτά που ήδη έχει κλειδώσει ο τρέχων agent
                .filter(c -> c.getStatus() != ReviewStatus.IN_PROGRESS ||
                        !currentAgentId.equals(c.getAssignedAgentId()))
                .map(this::mapToQueueItem)
                .toList();
    }

    // ΚΕΝΤΡΙΚΗ ΜΕΘΟΔΟΣ MAPPING (ΕΔΩ ΕΓΙΝΕ Η ΔΙΟΡΘΩΣΗ ΓΙΑ ΤΟ ΣΦΑΛΜΑ CONSTRUCTOR)
    private ReviewQueueItem mapToQueueItem(ReviewCase c) {
        var submission = submissionClient.getRawData(c.getCaseId()).orElse(null);
        var ai = aiClient.getAnalysisResult(c.getCaseId()).orElse(null);

        // Υπολογισμός χρόνου με ασφάλεια (Null check)
        int minsOpen = 0;
        if (c.getCreatedAt() != null) {
            minsOpen = (int) Duration.between(c.getCreatedAt(), Instant.now()).toMinutes();
        }

        // ΠΡΕΠΕΙ ΝΑ ΕΧΕΙ 6 ΠΑΡΑΜΕΤΡΟΥΣ ΑΚΡΙΒΩΣ ΜΕ ΑΥΤΗ ΤΗ ΣΕΙΡΑ:
        return new ReviewQueueItem(
                c.getCaseId(),                                          // 1. caseId
                submission != null ? submission.reporterName() : "Unknown", // 2. reporterName
                ai != null ? ai.severityLevel() : "PROCESSING",          // 3. severity
                c.getStatus().name(),                                   // 4. status
                minsOpen,                                               // 5. minutesOpen (long/int)
                c.getAssignedAgentId()                                  // 6. assignedAgentId (ΑΥΤΟ ΕΛΕΙΠΕ)
        );
    }
}