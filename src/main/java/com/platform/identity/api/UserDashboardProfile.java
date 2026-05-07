package com.platform.identity.api;

import com.platform.identity.domain.UserStatus;

import java.util.Set;
import java.util.List;

public record UserDashboardProfile(String id, String email, Set<String> roles, List<String> permissions, String fullName, UserStatus status) {}

