package com.platform.accident.review.integration;

import com.platform.integration.identity.DashboardDiscoveryPorts;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.repository.ReviewCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ReviewDashboardAdapter implements DashboardDiscoveryPorts.ReviewMetrics {

    private final ReviewCaseRepository reviewRepo;

    @Async
    @Override
    public CompletableFuture<Long> getTotalPendingQueue() {
        return CompletableFuture.completedFuture(reviewRepo.countByStatus(ReviewStatus.PENDING));
    }

    @Async
    @Override
    public CompletableFuture<Long> getAgentActiveLockCount(String agentId) {
        return CompletableFuture.completedFuture(
                reviewRepo.countByAssignedAgentIdAndStatus(agentId, ReviewStatus.IN_PROGRESS)
        );
    }
}