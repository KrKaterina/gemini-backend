package com.platform.integration.review;

import java.time.Instant;

/**
 * Data Snapshot from Module 1 (Submission) for the Human Agent dashboard.
 */
public record AccidentSnapshotView(
        String caseId,
        String description,
        String reporterId,
        double lat,
        double lng,
        String weather,
        String roadType,
        Instant occurrenceTime
) {}