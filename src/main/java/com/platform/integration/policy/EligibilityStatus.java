package com.platform.integration.policy;

/**
 * Type-safe response explaining the coverage decision.
 */
public record EligibilityStatus(
        boolean eligible,
        String reasonCode, // "EXPIRED", "NOT_FOUND", "PENDING_VERIFICATION", "VALID"
        String policyNumber,
        String providerCode
) {
    public static EligibilityStatus denied(String reason) {
        return new EligibilityStatus(false, reason, null, null);
    }
}
