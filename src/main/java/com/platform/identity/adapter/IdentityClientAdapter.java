package com.platform.identity.adapter;

import com.platform.identity.domain.*;
import com.platform.identity.repository.*;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdentityClientAdapter implements IdentityClient {

    private final UserAccountRepository userRepository;
    private final RevokedTokenRepository tokenBlacklist; // Added
    private final IdentityAuditRepository auditRepository; // Added
    private final JwtTokenUtil jwtUtil; // Shared utility

    @Override
    public Optional<IdentityContext> validateSession(String token) {
        if (tokenBlacklist.existsByToken(token)) return Optional.empty();

        return jwtUtil.parseToken(token).flatMap(claims -> {
            String userId = claims.getSubject();
            return userRepository.findById(userId)
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .map(u -> {
                        // FIX: Derive the permissions list from the roles
                        // before instantiating the 6-argument Record
                        var rolesList = List.copyOf(u.getRoles());
                        var permissionsList = u.getRoles().stream()
                                .flatMap(role -> PermissionRegistry.getPermissionsForRole(role).stream())
                                .distinct()
                                .toList();

                        return new IdentityContext(
                                u.getUserId(),
                                u.getUsername(),
                                rolesList,          // Param 3: List<String> roles
                                permissionsList,    // Param 4: List<String> permissions
                                u.getExternalReference(), // Param 5: String
                                true                // Param 6: boolean (isActive)
                        );
                    });
        });
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        return userRepository.findById(userId)
                .map(u -> PermissionRegistry.hasPermission(u.getRoles(), permission))
                .orElse(false);
    }

    @Async // Constraint: Log without blocking security execution
    @Override
    public void logSecurityEvent(String userId, String action, String details) {
        auditRepository.save(new IdentityEvent(null, userId, action, Instant.now(),"INTERNAL",details));
    }
}