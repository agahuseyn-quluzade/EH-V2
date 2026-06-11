package com.eHealthInsurance.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ortaq JWT konfiqurasiyası. Servislər {@code application.yaml}-da belə qura bilər:
 * <pre>
 * ehi:
 *   security:
 *     jwt:
 *       secret: ${JWT_SECRET}
 *       # və ya (gələcəkdə JWKS-ə keçəndə):
 *       jwk-set-uri: http://iam:8081/.well-known/jwks.json
 * </pre>
 */
@ConfigurationProperties("ehi.security.jwt")
public class JwtSecurityProperties {

    /** Base64-encoded HMAC açarı (Variant 1). secret və ya jwkSetUri-dən biri tələb olunur. */
    private String secret;

    /** JWKS endpoint URL-i (Variant 2 — gələcəkdə). */
    private String jwkSetUri;

    private String issuer = "https://iam.e-health-insurance.local";

    private String audience = "e-health-insurance-api";

    private String serviceIssuer = "https://services.e-health-insurance.local";

    private String serviceAudience = "e-health-insurance-internal";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getServiceIssuer() {
        return serviceIssuer;
    }

    public void setServiceIssuer(String serviceIssuer) {
        this.serviceIssuer = serviceIssuer;
    }

    public String getServiceAudience() {
        return serviceAudience;
    }

    public void setServiceAudience(String serviceAudience) {
        this.serviceAudience = serviceAudience;
    }
}
