package com.platform.accident.review.service;

import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.repository.CaseReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCleanupTask {

    private final CaseReviewRepository repository;

    @Scheduled(fixedDelayString = "${app.review.lock-cleanup-ms:300000}") // Default 5 mins
    public void cleanupExpiredLocks() {
        log.info("Starting stale review lock cleanup...");

        repository.findAll().stream()
                .filter(review -> review.getStatus() == ReviewStatus.IN_PROGRESS)
                .filter(review -> review.getLockExpiresAt() != null && review.getLockExpiresAt().isBefore(Instant.now()))
                .forEach(review -> {
                    log.info("Releasing expired lock on case: {} (Agent: {})", review.getCaseId(), review.getLockedBy());
                    review.setLockedBy(null);
                    review.setLockedAt(null);
                    review.setLockExpiresAt(null);
                    review.setStatus(ReviewStatus.WAITING);
                    repository.save(review);
                });
    }
}
