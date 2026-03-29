package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;

public record CaseFileView(
        String caseId,
        String rawDescription,
        Location location,
        String aiSummary,
        String aiSeverity,
        String weatherCondition,
        String roadType,
        boolean enrichmentUnavailable
) {}