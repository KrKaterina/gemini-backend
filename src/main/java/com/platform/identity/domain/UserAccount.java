package com.platform.identity.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Set;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "user_accounts")
public class UserAccount {
    @Id private String userId;

    @Indexed(unique = true)
    private String username;

    private String passwordHash;
    private Set<String> roles;
    private UserStatus status;
    private String externalReference; // Linked to Customer ID in Insurance DB

    // Security Monitoring
    private int failedAttempts;
    private Instant lockoutExpiry;
    private Instant lastLogin;

    private Instant createdAt;
    private UserProfile profile;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private String firstName;
        private String lastName;
    }
}