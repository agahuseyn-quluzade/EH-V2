package com.eHealthInsurance.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EhiJwtValidatorTest {

    private final JwtSecurityProperties properties = properties();
    private final EhiJwtValidator validator = new EhiJwtValidator(properties);

    @Test
    void acceptsLegacyUserTokenWithUserId() {
        Jwt jwt = baseJwt()
                .claim("user_id", UUID.randomUUID().toString())
                .claim("role", "MEMBER")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsUserTokenWithoutUserId() {
        Jwt jwt = baseJwt()
                .claim("role", "MEMBER")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void acceptsServiceTokenWithIssuerAndAudience() {
        String serviceId = UUID.randomUUID().toString();
        Jwt jwt = baseJwt()
                .issuer(properties.getServiceIssuer())
                .audience(List.of(properties.getServiceAudience()))
                .subject(serviceId)
                .claim("user_id", serviceId)
                .claim("role", "CLAIM_SERVICE")
                .claim("token_type", "service")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsServiceRoleWithoutServiceIssuerAndAudience() {
        Jwt jwt = baseJwt()
                .claim("user_id", UUID.randomUUID().toString())
                .claim("role", "CLAIM_SERVICE")
                .build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private Jwt.Builder baseJwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("subject")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60));
    }

    private JwtSecurityProperties properties() {
        JwtSecurityProperties props = new JwtSecurityProperties();
        props.setServiceIssuer("https://services.e-health-insurance.local");
        props.setServiceAudience("e-health-insurance-internal");
        return props;
    }
}
