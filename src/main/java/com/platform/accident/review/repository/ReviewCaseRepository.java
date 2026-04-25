package com.platform.accident.review.repository;

import com.platform.accident.review.domain.ReviewAuditEntry;
import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewCaseRepository extends MongoRepository<ReviewCase, String> {

    Optional<ReviewCase> findByCaseId(String caseId);

    List<ReviewCase> findByLockedAtBeforeAndStatus(Instant expiryTime, com.platform.accident.review.domain.ReviewStatus status);

    List<ReviewCase> findByStatus(ReviewStatus status);
    List<ReviewCase> findByAssignedAgentIdAndStatus(String agentId, ReviewStatus status);
}
