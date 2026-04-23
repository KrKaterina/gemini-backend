package com.platform.identity.api.dto;

public record StaffOnboardRequest(
        String username,
        String password,
        String firstName,
        String lastName,
        java.util.Set<String> roles // e.g., ["ROLE_AGENT"]
) {}
