package com.platform.policy.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "insurance_declarations")
public class InsuranceDeclaration {
    @Id
    private String id;

    @Indexed
    private String userId; // Identity context reference

    private String providerCode; // e.g., "ALLIANZ", "GEICO"
    private String policyNumber;

    private Instant validFrom;

    // Reporting audit trail
    private Instant reportedExpirationDate;

    // Agent verification logic
    private Instant verifiedExpirationDate;
    private String verifiedByAgentId;
    private Instant verifiedAt;

    private String proofAssetId; // Reference to Media Module 5
    private DeclarationStatus status;
    private Instant createdAt;

    @Version
    private Long version; // Optimistic locking
}