package com.platform.accident.review.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "case_reviews")
public class CaseReview {
    @Id
    private String id;
    private String caseId;

    private ReviewStatus status;
    private String lockedBy;
    private Instant lockedAt;
    private Instant lockExpiresAt;

    @Builder.Default
    private List<Adjustment> adjustments = new ArrayList<>();
    @Builder.Default
    private List<ReviewAuditEntry> auditTrail = new ArrayList<>();

    @Version
    private Long version;

    public boolean isLocked() {
        return lockedAt != null && Instant.now().isBefore(lockExpiresAt);
    }
}
