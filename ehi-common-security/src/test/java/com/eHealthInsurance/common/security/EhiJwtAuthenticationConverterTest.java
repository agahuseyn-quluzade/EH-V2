package com.eHealthInsurance.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EhiJwtAuthenticationConverterTest {

    private final EhiJwtAuthenticationConverter converter = new EhiJwtAuthenticationConverter();

    @Test
    void usesUserIdAsPrincipalAndNormalizesRoles() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("user@example.com")
                .claim("user_id", userId.toString())
                .claim("role", "member")
                .claim("roles", List.of("ADMIN", "ROLE_STAFF"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        JwtAuthenticationToken auth = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo(userId.toString());
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER", "ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    void fallsBackToSubjectWhenUserIdMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("legacy-subject")
                .claim("role", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        JwtAuthenticationToken auth = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(auth.getName()).isEqualTo("legacy-subject");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
