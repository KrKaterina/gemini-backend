package com.platform.accident.submission.api.dto;

import java.time.Instant;

public record AccidentSubmissionResponse(
        String caseId,
        String status,
        Instant submittedAt
) {}
