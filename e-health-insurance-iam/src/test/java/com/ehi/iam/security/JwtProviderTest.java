package com.ehi.iam.security;

import com.ehi.iam.config.JwtProperties;
import com.ehi.infra.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;
    // Must be ≥ 32 bytes for HMAC-SHA256
    private static final String SECRET = "test-secret-key-that-is-at-least-256-bits-long-for-hmacsha256-tests";

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenExpiration(900_000L);
        props.setRefreshTokenExpiration(604_800_000L);
        jwtProvider = new JwtProvider(props);
    }

    @Test
    void accessToken_containsUserIdAndRoleClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateAccessToken(userId, "test@example.com", UserRole.CUSTOMER);

        assertThat(jwtProvider.isTokenValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.CUSTOMER);
        assertThat(jwtProvider.getEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void refreshToken_containsUserIdAndRoleClaims() {
        UUID userId = UUID.randomUUID();
        String token = jwtProvider.generateRefreshToken(userId, "refresh@example.com", UserRole.ADMIN);

        assertThat(jwtProvider.isTokenValid(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(userId);
        assertThat(jwtProvider.getRole(token)).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void isTokenValid_returnsFalse_forTokenSignedWithWrongSecret() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("different-secret-key-also-long-enough-for-hmacsha256-signing-ok");
        otherProps.setAccessTokenExpiration(900_000L);
        otherProps.setRefreshTokenExpiration(604_800_000L);
        JwtProvider otherProvider = new JwtProvider(otherProps);

        String foreignToken = otherProvider.generateAccessToken(UUID.randomUUID(), "x@y.com", UserRole.CUSTOMER);

        assertThat(jwtProvider.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forMalformedToken() {
        assertThat(jwtProvider.isTokenValid("not.a.jwt")).isFalse();
        assertThat(jwtProvider.isTokenValid("")).isFalse();
    }
}
