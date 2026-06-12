package com.ehi.iam.security;

import com.ehi.iam.config.JwtProperties;
import com.ehi.infra.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public String generateAccessToken(UUID userId, String email, UserRole role) {
        return generateToken(userId, email, role, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(UUID userId, String email, UserRole role) {
        return generateToken(userId, email, role, jwtProperties.getRefreshTokenExpiration());
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(extractClaims(token).get("userId", String.class));
    }

    public UserRole getRole(String token) {
        return UserRole.valueOf(extractClaims(token).get("role", String.class));
    }

    private String generateToken(UUID userId, String email, UserRole role, long expirationMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
