package com.platform.identity.api.dto;

public record AgentDashboardResponse(
        String role,
        String fullName,
        long totalPending,
        long myActiveWork
) implements DashboardResponse {}
