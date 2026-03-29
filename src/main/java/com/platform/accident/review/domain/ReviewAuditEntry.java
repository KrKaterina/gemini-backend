package com.platform.accident.review.domain;

import lombok.Value;

import java.time.Instant;

@Value
public class ReviewAuditEntry {
    Instant timestamp;
    String agentId;
    String action;
}
