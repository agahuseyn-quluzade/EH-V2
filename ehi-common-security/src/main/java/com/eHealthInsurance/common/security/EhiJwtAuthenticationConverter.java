package com.eHealthInsurance.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class EhiJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String principal = jwt.getClaimAsString("user_id");
        if (!StringUtils.hasText(principal)) {
            principal = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), principal);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        String role = jwt.getClaimAsString("role");
        if (StringUtils.hasText(role)) {
            authorities.add(toAuthority(role));
        }

        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> roles) {
            roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .filter(StringUtils::hasText)
                    .map(this::toAuthority)
                    .forEach(authorities::add);
        }

        return authorities;
    }

    private GrantedAuthority toAuthority(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return new SimpleGrantedAuthority(normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized);
    }
}
