package com.platform.accident.review.service;

import com.platform.accident.submission.integration.CaseFileView;
import org.springframework.stereotype.Component;

@Component
public class VerificationPolicy {

    /**
     * Encapsulates Insurance Business Rules.
     * Prevents verification if enrichment failed or data is corrupt.
     */
    public void validateReadyForVerification(CaseFileView caseFile) {
        if (caseFile.enrichmentUnavailable()) {
            // Business rule: Human must manually fetch weather if API failed
            // For now, we block it to force data completeness
            throw new IllegalStateException("Verification blocked: Weather/Road context is missing");
        }

        if (caseFile.aiSummary() == null || caseFile.aiSummary().isBlank()) {
            throw new IllegalStateException("Verification blocked: AI reconstruction is required");
        }
    }
}
