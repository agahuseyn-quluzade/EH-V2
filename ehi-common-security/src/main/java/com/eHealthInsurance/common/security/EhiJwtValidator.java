package com.eHealthInsurance.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.Set;

public class EhiJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final String ERROR_CODE = "invalid_token";
    private static final Set<String> SERVICE_ROLES = Set.of(
            "CLAIM_SERVICE",
            "POLICY_SERVICE",
            "AI_SERVICE",
            "IAM_SERVICE",
            "NOTIFICATION_SERVICE",
            "PAYMENT_SERVICE",
            "HEALTH_RECORD_SERVICE"
    );

    private final JwtSecurityProperties properties;

    public EhiJwtValidator(JwtSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String role = token.getClaimAsString("role");
        if (isServiceToken(token, role)) {
            return validateServiceToken(token, role);
        }
        return validateUserToken(token, role);
    }

    private OAuth2TokenValidatorResult validateUserToken(Jwt token, String role) {
        if (!StringUtils.hasText(token.getClaimAsString("user_id"))) {
            return failure("User token must contain user_id claim");
        }
        if (isServiceRole(role)) {
            return failure("Service role requires service token issuer and audience");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult validateServiceToken(Jwt token, String role) {
        if (!isServiceRole(role)) {
            return failure("Service token role is not allowed");
        }
        if (!properties.getServiceIssuer().equals(token.getIssuer() == null ? null : token.getIssuer().toString())) {
            return failure("Service token issuer is invalid");
        }
        if (!token.getAudience().contains(properties.getServiceAudience())) {
            return failure("Service token audience is invalid");
        }
        if (!StringUtils.hasText(token.getClaimAsString("user_id"))) {
            return failure("Service token must contain service user_id");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private boolean isServiceToken(Jwt token, String role) {
        return "service".equals(token.getClaimAsString("token_type"))
                || "service".equals(token.getClaimAsString("typ"))
                || isServiceRole(role);
    }

    private boolean isServiceRole(String role) {
        return StringUtils.hasText(role) && SERVICE_ROLES.contains(role.toUpperCase());
    }

    private OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, description, null));
    }
}
