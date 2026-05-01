package com.platform.policy.api;

import java.time.Instant;

public record VerificationRequest(Instant verifiedExpiry,String status ) {
}
