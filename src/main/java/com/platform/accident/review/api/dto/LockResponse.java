package com.platform.accident.review.api.dto;

public record LockResponse(
        String caseId,
        String agentId,
        String expiresAt
) {}