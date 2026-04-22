package com.platform.identity.domain;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Builder
@Document(collection = "revoked_tokens")
public record RevokedToken(
        @Id String id,
        @Indexed(unique = true) String token,
        @Indexed(expireAfterSeconds = 0) Instant expiryTime // MongoDB TTL
) {}