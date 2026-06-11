package com.eHealthInsurance.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.Assert;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceTokenProvider {

    private static final UUID CLAIM_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000C141");
    private static final UUID POLICY_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000901C");
    private static final UUID AI_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000A1");
    private static final UUID IAM_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-0000000001A1");
    private static final UUID NOTIFICATION_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000F1");
    private static final UUID PAYMENT_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000A711");
    private static final UUID HEALTH_RECORD_SERVICE_ID = UUID.fromString("00000000-0000-0000-0000-00000000A712");

    private static final long TOKEN_TTL_MS = 60L * 60L * 1000L;
    private static final long REFRESH_BEFORE_EXPIRY_MS = 60L * 1000L;

    private final SecretKey signingKey;
    private final JwtSecurityProperties properties;
    private final ConcurrentHashMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    public ServiceTokenProvider(JwtSecurityProperties properties) {
        Assert.notNull(properties, "JwtSecurityProperties is required");
        Assert.hasText(properties.getSecret(), "Service token secret is required");
        byte[] keyBytes = Base64.getDecoder().decode(properties.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.properties = properties;
    }

    public String issue(String role) {
        String normalizedRole = normalizeRole(role);
        CachedToken existing = cache.get(normalizedRole);
        long now = System.currentTimeMillis();
        if (existing != null && existing.expiresAt() - REFRESH_BEFORE_EXPIRY_MS > now) {
            return existing.token();
        }
        return mint(normalizedRole, now);
    }

    private synchronized String mint(String role, long now) {
        CachedToken existing = cache.get(role);
        if (existing != null && existing.expiresAt() - REFRESH_BEFORE_EXPIRY_MS > now) {
            return existing.token();
        }

        long expiresAt = now + TOKEN_TTL_MS;
        UUID serviceId = serviceIdFor(role);
        String token = Jwts.builder()
                .issuer(properties.getServiceIssuer())
                .claim("aud", properties.getServiceAudience())
                .claim("token_type", "service")
                .claim("typ", "service")
                .claim("user_id", serviceId.toString())
                .claim("role", role)
                .subject(serviceId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(expiresAt))
                .signWith(signingKey)
                .compact();
        cache.put(role, new CachedToken(token, expiresAt));
        return token;
    }

    private String normalizeRole(String role) {
        Assert.hasText(role, "Service role is required");
        return role.toUpperCase(Locale.ROOT);
    }

    private UUID serviceIdFor(String role) {
        return switch (role) {
            case "CLAIM_SERVICE" -> CLAIM_SERVICE_ID;
            case "POLICY_SERVICE" -> POLICY_SERVICE_ID;
            case "AI_SERVICE" -> AI_SERVICE_ID;
            case "IAM_SERVICE" -> IAM_SERVICE_ID;
            case "NOTIFICATION_SERVICE" -> NOTIFICATION_SERVICE_ID;
            case "PAYMENT_SERVICE" -> PAYMENT_SERVICE_ID;
            case "HEALTH_RECORD_SERVICE" -> HEALTH_RECORD_SERVICE_ID;
            default -> throw new IllegalArgumentException("Unknown service role: " + role);
        };
    }

    private record CachedToken(String token, long expiresAt) {
    }
}
