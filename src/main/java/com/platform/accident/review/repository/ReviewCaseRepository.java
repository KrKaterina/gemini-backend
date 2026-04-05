package com.platform.accident.review.repository;

import com.platform.accident.review.domain.ReviewAuditEntry;
import com.platform.accident.review.domain.ReviewCase;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewCaseRepository extends MongoRepository<ReviewCase, String> {

    Optional<ReviewCase> findByCaseId(String caseId);

    // Βρίσκει υποθέσεις όπου το κλείδωμα έχει λήξει
    List<ReviewCase> findByLockedAtBeforeAndStatus(Instant expiryTime, com.platform.accident.review.domain.ReviewStatus status);
}
