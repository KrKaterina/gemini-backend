package com.platform.accident.review.repository;

import com.platform.accident.review.domain.ReviewAuditEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewAuditRepository extends MongoRepository<ReviewAuditEntry, String> {
    List<ReviewAuditEntry> findByCaseId(String caseId);
}
