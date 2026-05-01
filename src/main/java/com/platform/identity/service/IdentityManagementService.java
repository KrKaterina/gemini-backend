package com.platform.identity.service;

import com.platform.identity.api.UserDashboardProfile;
import com.platform.identity.api.dto.RegistrationRequest;
import com.platform.identity.domain.*;
import com.platform.identity.exception.AccountLockedException;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.identity.exception.UserConflictException;
import com.platform.identity.repository.UserAccountRepository;
import com.platform.integration.identity.IdentityClient;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityManagementService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final IdentityClient identityClient;

    @Value("${platform.security.jwt.secret}")
    private String jwtSecret;

    public String authenticate(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (account.getStatus() == UserStatus.LOCKED) {
            throw new AccountLockedException("Account is locked");
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            handleFailedLogin(account);
            //throw new RuntimeException("Authentication Failed");
            throw new UnauthorizedException("Invalid credentials");
        }

        resetFailedLogins(account);
        return generateToken(account);
    }

    private String generateToken(UserAccount user) {
        return Jwts.builder()
                .setSubject(user.getUserId())
                .claim("roles", user.getRoles())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 Hour
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    private void handleFailedLogin(UserAccount account) {
        account.setFailedAttempts(account.getFailedAttempts() + 1);
        if (account.getFailedAttempts() >= 5) {
            account.setStatus(UserStatus.LOCKED);
            account.setLockoutExpiry(Instant.now().plusSeconds(1800)); // 30 Min lockout
        }
        userRepository.save(account);
    }

    private void resetFailedLogins(UserAccount account) {
        account.setFailedAttempts(0);
        account.setLockoutExpiry(null);
        userRepository.save(account);
    }

    @Transactional
    public void registerUser(RegistrationRequest req, Set<String> roles) {
        if (userRepository.existsByUsername(req.username())) {
            //throw new RuntimeException("Username already exists");
            throw new UserConflictException("Email address exists");
        }

        UserAccount account = UserAccount.builder()
                .userId(UUID.randomUUID().toString())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .roles(roles)
                .status(UserStatus.ACTIVE)
                .profile(new UserAccount.UserProfile(req.firstName(), req.lastName()))
                .externalReference(req.externalReference())
                .createdAt(Instant.now())
                .build();

        userRepository.save(account);
        identityClient.logSecurityEvent(account.getUserId(), "USER_REGISTERED", "Roles assigned: " + roles);
    }

    public List<UserDashboardProfile> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> {
                    String fullName = (u.getProfile() != null)
                            ? u.getProfile().getFirstName() + " " + u.getProfile().getLastName()
                            : u.getUsername(); // Fallback στο email αν δεν υπάρχει όνομα

                    var permissions = u.getRoles().stream()
                            .flatMap(role -> PermissionRegistry.getPermissionsForRole(role).stream())
                            .distinct()
                            .toList();

                    return new UserDashboardProfile(
                            u.getUserId(),
                            u.getUsername(),
                            u.getRoles(),
                            permissions,
                            fullName
                    );
                })
                .toList();
    }
}