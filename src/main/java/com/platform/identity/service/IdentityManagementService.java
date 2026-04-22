package com.platform.identity.service;

import com.platform.identity.domain.*;
import com.platform.identity.repository.UserAccountRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IdentityManagementService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${platform.security.jwt.secret}")
    private String jwtSecret;

    public String authenticate(String username, String password) {
        UserAccount account = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authentication Failed"));

        if (account.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("Account is locked");
        }

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            handleFailedLogin(account);
            throw new RuntimeException("Authentication Failed");
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
}