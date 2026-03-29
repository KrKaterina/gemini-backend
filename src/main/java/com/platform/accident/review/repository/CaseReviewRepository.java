package com.platform.accident.review.repository;

import com.platform.accident.review.domain.CaseReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CaseReviewRepository extends MongoRepository<CaseReview, String> {
    Optional<CaseReview> findByCaseId(String caseId);
}