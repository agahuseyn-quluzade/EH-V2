package com.ehi.gw.security;

import com.ehi.gw.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmacsha256-tests";

    private JwtProvider jwtProvider;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProvider = new JwtProvider(jwtProperties);
    }

    private String token(String secret, String userId, String role, long expirationMillis) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@example.com")
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(key)
                .compact();
    }

    @Test
    void validToken_isValid_andClaimsExtracted() {
        UUID userId = UUID.randomUUID();
        String token = token(SECRET, userId.toString(), "CUSTOMER", 900_000L);

        assertThat(jwtProvider.isTokenValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(userId.toString());
        assertThat(jwtProvider.getRole(token)).isEqualTo("CUSTOMER");
    }

    @Test
    void isTokenValid_returnsFalse_forTokenSignedWithWrongSecret() {
        String foreignToken = token("different-secret-key-also-long-enough-for-hmacsha256-signing-ok",
                UUID.randomUUID().toString(), "ADMIN", 900_000L);

        assertThat(jwtProvider.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        String expiredToken = token(SECRET, UUID.randomUUID().toString(), "STAFF", -1_000L);

        assertThat(jwtProvider.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        assertThat(jwtProvider.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtProvider.isTokenValid("")).isFalse();
    }
}
