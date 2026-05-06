package com.platform.integration.review;

import java.time.Instant;
import java.util.List;

/**
 * Data Snapshot from Module 1 (Submission) for the Human Agent dashboard.
 */
public record AccidentSnapshotView(
        String caseId,
        String description,
        String reporterName,
        double lat,
        double lng,
        String weather,
        String roadType,
        Instant occurrenceTime,
        List<String> assetIds,
        Double temperature,
        String address,
        Integer speedLimit
) {}
