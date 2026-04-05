package com.platform.accident.review.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@Document(collection = "review_audit_logs")
public class ReviewAuditEntry {
    @Id
    private String id;
    private String caseId;
    private String agentId;
    private String action; // π.χ. "LOCKED", "VERIFIED", "REJECTED"
    private Instant timestamp;
    private String detail;
}