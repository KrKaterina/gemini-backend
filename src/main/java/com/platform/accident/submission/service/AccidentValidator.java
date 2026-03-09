package com.platform.accident.submission.service;

import com.platform.accident.submission.api.dto.AccidentReportRequest;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class AccidentValidator {
    // Move hardcoded strings to constants; in production use MessageSource
    public static final String ERR_FUTURE_DATE = "Accident occurrence time cannot be in the future.";
    public static final String ERR_INVALID_COORDS = "Latitude must be between -90 and 90.";

    public void validateInput(AccidentReportInput input) {
        if (input.occurrenceTime().isAfter(java.time.Instant.now())) {
            throw new IllegalArgumentException(ERR_FUTURE_DATE);
        }
        if (input.location().lat() < -90 || input.location().lat() > 90) {
            throw new IllegalArgumentException(ERR_INVALID_COORDS);
        }
        if (input.description() == null || input.description().isBlank()) {
            throw new IllegalArgumentException("Description is mandatory");
        }
    }

    public void verifyAssetOwnership(String userId, java.util.List<String> assetIds) {
        // Stub: Logic to call Media Module and verify userId uploaded these specific assets
        if (assetIds.size() > 10) {
            throw new com.platform.accident.submission.exception.AssetOwnershipException("Too many assets for a single report.");
        }
    }
}
