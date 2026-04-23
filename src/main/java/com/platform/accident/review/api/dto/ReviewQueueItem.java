package com.platform.accident.review.api.dto;

public record ReviewQueueItem(
        String caseId,
        String reporterName,
        String severity,   // Derived from AI analysis result
        String status,     // PENDING vs IN_PROGRESS
        long minutesOpen   // Calculated from creation time
) {}
