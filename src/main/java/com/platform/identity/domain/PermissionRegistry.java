package com.platform.identity.domain;

import java.util.Map;
import java.util.Set;

/**
 * Central authority for Role-to-Permission mapping.
 * Other modules check for these 'Action' strings.
 */
public class PermissionRegistry {
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "ROLE_CUSTOMER", Set.of("ACCIDENT_REPORT_CREATE", "ACCIDENT_REPORT_VIEW_OWN", "ASSET_UPLOAD"),
            "ROLE_AGENT",    Set.of("ACCIDENT_REPORT_VIEW_ALL", "CASE_REVIEW_LOCK", "CASE_REVIEW_VERIFY", "ASSET_VIEW"),
            "ROLE_ADMIN",    Set.of("USER_MANAGEMENT", "SYSTEM_AUDIT_VIEW")
    );

    public static boolean hasPermission(Set<String> userRoles, String permission) {
        return userRoles.stream()
                .anyMatch(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).contains(permission));
    }
}