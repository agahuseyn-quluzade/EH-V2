package com.eHealthInsurance.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * Validated token-dən çıxarılmış istifadəçi kimliyi.
 * <p>
 * Controller-lərdə birbaşa istifadə üçün:
 * <pre>
 *     AuthenticatedUser user = AuthenticatedUser.current();
 * </pre>
 * və ya {@code Authentication} parametri ilə:
 * <pre>
 *     AuthenticatedUser user = AuthenticatedUser.from(authentication);
 * </pre>
 */
public record AuthenticatedUser(UUID userId, String role) {

    public static AuthenticatedUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Kimlik təsdiqlənməyib");
        }
        return from(auth);
    }

    public static AuthenticatedUser from(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("JWT autentifikasiyası tələb olunur");
        }
        Jwt token = jwtAuth.getToken();
        String userIdClaim = token.getClaimAsString("user_id");
        if (userIdClaim == null) {
            userIdClaim = token.getSubject();
        }
        if (userIdClaim == null) {
            throw new IllegalStateException("Token-də user_id və ya sub tapılmadı");
        }
        UUID userId;
        try {
            userId = UUID.fromString(userIdClaim);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Token-dəki user_id UUID formatında deyil: " + userIdClaim, ex);
        }
        String role = token.getClaimAsString("role");
        return new AuthenticatedUser(userId, role == null ? "UNKNOWN" : role);
    }
}
