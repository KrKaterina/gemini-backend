package com.platform.accident.review.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "case_reviews")
public class ReviewCase {
    @Id
    private String id;
    private String caseId;
    private String assignedAgentId;
    private Instant lockedAt;
    private Instant createdAt;
    private ReviewStatus status;
    private Map<String, Object> corrections; // Stores agent overrides
}