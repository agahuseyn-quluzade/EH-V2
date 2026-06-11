package com.eHealthInsurance.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

public class TrustedGatewayHeaderFilter extends OncePerRequestFilter {

    private final GatewayHeaderSecurityProperties properties;
    private final JwtSecurityProperties jwtProperties;

    public TrustedGatewayHeaderFilter(GatewayHeaderSecurityProperties properties,
                                      JwtSecurityProperties jwtProperties) {
        this.properties = properties;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled() || !containsIdentityHeader(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isTrusted(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(new StrippedIdentityHeaderRequest(request), response);
    }

    private boolean containsIdentityHeader(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader(GatewayIdentityHeaders.USER_ID))
                || StringUtils.hasText(request.getHeader(GatewayIdentityHeaders.USER_ROLE));
    }

    private boolean isTrusted(HttpServletRequest request) {
        String userId = request.getHeader(GatewayIdentityHeaders.USER_ID);
        String role = request.getHeader(GatewayIdentityHeaders.USER_ROLE);
        String gateway = request.getHeader(GatewayIdentityHeaders.GATEWAY);
        String timestamp = request.getHeader(GatewayIdentityHeaders.TIMESTAMP);
        String signature = request.getHeader(GatewayIdentityHeaders.SIGNATURE);
        String secret = signingSecret();

        if (!GatewayIdentityHeaders.GATEWAY_VALUE.equals(gateway)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(signature)
                || !StringUtils.hasText(secret)
                || timestampOutsideWindow(timestamp)) {
            return false;
        }

        String expected = GatewayIdentityHeaders.sign(secret, userId, role, timestamp);
        return GatewayIdentityHeaders.signatureMatches(expected, signature);
    }

    private String signingSecret() {
        return StringUtils.hasText(properties.getSecret()) ? properties.getSecret() : jwtProperties.getSecret();
    }

    private boolean timestampOutsideWindow(String value) {
        try {
            long timestamp = Long.parseLong(value);
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - timestamp) > properties.getMaxSkewSeconds();
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private static class StrippedIdentityHeaderRequest extends HttpServletRequestWrapper {

        private static final Set<String> STRIPPED_HEADERS = Set.of(
                GatewayIdentityHeaders.USER_ID.toLowerCase(Locale.ROOT),
                GatewayIdentityHeaders.USER_ROLE.toLowerCase(Locale.ROOT),
                GatewayIdentityHeaders.GATEWAY.toLowerCase(Locale.ROOT),
                GatewayIdentityHeaders.TIMESTAMP.toLowerCase(Locale.ROOT),
                GatewayIdentityHeaders.SIGNATURE.toLowerCase(Locale.ROOT)
        );

        StrippedIdentityHeaderRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return isStripped(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return isStripped(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Vector<String> names = new Vector<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!isStripped(name)) {
                    names.add(name);
                }
            }
            return names.elements();
        }

        private boolean isStripped(String name) {
            return name != null && STRIPPED_HEADERS.contains(name.toLowerCase(Locale.ROOT));
        }
    }
}
