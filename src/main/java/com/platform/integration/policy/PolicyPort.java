package com.platform.integration.policy;

import java.time.Instant;
import java.util.Optional;

public interface PolicyPort {
    /**
     * Used by Submission Module to check if reporting is allowed.
     */
  //  boolean isEligibleAtTime(String userId, Instant incidentTime);

    EligibilityStatus checkEligibility(String userId, Instant occurrenceTime);

    /**
     * Used by Review Module to gather final insurer data for notification.
     */
    Optional<InsurerSnapshot> getActiveDeclaration(String userId, Instant incidentTime);
}

