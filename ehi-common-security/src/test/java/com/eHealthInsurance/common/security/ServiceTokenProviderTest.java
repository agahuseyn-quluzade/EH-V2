package com.eHealthInsurance.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceTokenProviderTest {

    @Test
    void issuesTokenAcceptedByCommonDecoder() {
        JwtSecurityProperties properties = properties();
        ServiceTokenProvider provider = new ServiceTokenProvider(properties);
        JwtDecoder decoder = new EhiResourceServerAutoConfiguration().ehiJwtDecoder(properties);

        Jwt jwt = decoder.decode(provider.issue("claim_service"));

        assertThat(jwt.getClaimAsString("token_type")).isEqualTo("service");
        assertThat(jwt.getIssuer().toString()).isEqualTo(properties.getServiceIssuer());
        assertThat(jwt.getAudience()).containsExactly(properties.getServiceAudience());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CLAIM_SERVICE");
        assertThat(jwt.getClaimAsString("user_id")).isNotBlank();
    }

    private JwtSecurityProperties properties() {
        JwtSecurityProperties props = new JwtSecurityProperties();
        props.setSecret(Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)));
        props.setServiceIssuer("https://services.e-health-insurance.local");
        props.setServiceAudience("e-health-insurance-internal");
        return props;
    }
}
