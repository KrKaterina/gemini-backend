package com.platform.policy.api;

import java.time.Instant;

public record DeclarationSubmission(String providerCode, String policyNumber, Instant expiryDate, String proofAssetId) {}

