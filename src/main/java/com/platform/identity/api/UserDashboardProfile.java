package com.platform.identity.api;

import java.util.Set;

public record UserDashboardProfile(String id, String email, Set<String> roles, String fullName) {}

