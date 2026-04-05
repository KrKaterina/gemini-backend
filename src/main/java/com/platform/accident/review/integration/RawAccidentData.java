package com.platform.accident.review.integration;

import com.platform.accident.submission.domain.Location; // External Shared Record
import java.time.Instant;

/**
 * Port to view raw accident data from the Submission Module.
 */
public record RawAccidentData(
        String description,
        Location location,
        Instant occurrenceTime
) {}