package com.eHealthInsurance.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedGatewayHeaderFilterTest {

    private static final String SECRET = "gateway-header-secret";

    @Test
    void stripsSpoofedIdentityHeadersWithoutGatewaySignature() throws Exception {
        TrustedGatewayHeaderFilter filter = filter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(GatewayIdentityHeaders.USER_ID, UUID.randomUUID().toString());
        request.addHeader(GatewayIdentityHeaders.USER_ROLE, "ADMIN");
        AtomicReference<HttpServletRequest> seenRequest = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), capture(seenRequest));

        assertThat(seenRequest.get().getHeader(GatewayIdentityHeaders.USER_ID)).isNull();
        assertThat(seenRequest.get().getHeader(GatewayIdentityHeaders.USER_ROLE)).isNull();
    }

    @Test
    void preservesGatewaySignedIdentityHeaders() throws Exception {
        TrustedGatewayHeaderFilter filter = filter();
        String userId = UUID.randomUUID().toString();
        String role = "MEMBER";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(GatewayIdentityHeaders.USER_ID, userId);
        request.addHeader(GatewayIdentityHeaders.USER_ROLE, role);
        request.addHeader(GatewayIdentityHeaders.GATEWAY, GatewayIdentityHeaders.GATEWAY_VALUE);
        request.addHeader(GatewayIdentityHeaders.TIMESTAMP, timestamp);
        request.addHeader(GatewayIdentityHeaders.SIGNATURE,
                GatewayIdentityHeaders.sign(SECRET, userId, role, timestamp));
        AtomicReference<HttpServletRequest> seenRequest = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), capture(seenRequest));

        assertThat(seenRequest.get().getHeader(GatewayIdentityHeaders.USER_ID)).isEqualTo(userId);
        assertThat(seenRequest.get().getHeader(GatewayIdentityHeaders.USER_ROLE)).isEqualTo(role);
    }

    private TrustedGatewayHeaderFilter filter() {
        GatewayHeaderSecurityProperties headerProps = new GatewayHeaderSecurityProperties();
        headerProps.setSecret(SECRET);
        JwtSecurityProperties jwtProps = new JwtSecurityProperties();
        return new TrustedGatewayHeaderFilter(headerProps, jwtProps);
    }

    private FilterChain capture(AtomicReference<HttpServletRequest> seenRequest) {
        return (ServletRequest request, ServletResponse response) ->
                seenRequest.set((HttpServletRequest) request);
    }
}
