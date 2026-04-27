package com.platform.identity.api.dto;

public record CustomerDashboardResponse(
        String role,
        String fullName,
        String onboardingStatus, // "ACTIVE", "PENDING", "NONE"
        long claimCount
) implements DashboardResponse {}
