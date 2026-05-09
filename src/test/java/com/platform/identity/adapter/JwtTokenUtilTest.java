package com.platform.identity.adapter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenUtilTest {

    private JwtTokenUtil jwtUtil;
    private final String VALID_SECRET = "MySuperSecretKeyForJWTThatIsLongEnoughToAvoidErrors1234567890123456";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtTokenUtil();
        // Χρησιμοποιούμε Reflection για να εισάγουμε το secret (αντικαθιστά το @Value)
        ReflectionTestUtils.setField(jwtUtil, "secret", VALID_SECRET);
    }

    @Test
    @DisplayName("Parse Token: Should successfully return claims for a valid token")
    void parseToken_Success() {
        // 1. Δημιουργούμε ένα πραγματικό Token για το τεστ
        String token = Jwts.builder()
                .setSubject("user-123")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(SignatureAlgorithm.HS512, VALID_SECRET)
                .compact();

        // 2. Εκτελούμε τη μέθοδο
        Optional<Claims> claims = jwtUtil.parseToken(token);

        // 3. Επαληθεύουμε
        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Parse Token: Should return empty Optional for malformed token strings")
    void parseToken_InvalidFormat() {
        // Act
        Optional<Claims> claims = jwtUtil.parseToken("not.a.valid.token.at.all");

        // Assert - Αυτό το τεστ καλύπτει το Catch block!
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("Parse Token: Should return empty Optional if the secret key does not match")
    void parseToken_SecretMismatch() {
        // 1. Token υπογεγραμμένο με ΔΙΑΦΟΡΕΤΙΚΟ μυστικό
        String wrongToken = Jwts.builder()
                .setSubject("hacker")
                .signWith(SignatureAlgorithm.HS512, "COMPLETELY_DIFFERENT_SECRET_KEY_XYZ_123456")
                .compact();

        // 2. Εκτελούμε
        Optional<Claims> claims = jwtUtil.parseToken(wrongToken);

        // 3. Επαληθεύουμε
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("Parse Token: Should return empty Optional for expired tokens")
    void parseToken_ExpiredToken() {
        // 1. Token που έληξε στο παρελθόν
        String expiredToken = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() - 100000)) // Έληξε πριν
                .signWith(SignatureAlgorithm.HS512, VALID_SECRET)
                .compact();

        Optional<Claims> claims = jwtUtil.parseToken(expiredToken);

        assertThat(claims).isEmpty();
    }
}