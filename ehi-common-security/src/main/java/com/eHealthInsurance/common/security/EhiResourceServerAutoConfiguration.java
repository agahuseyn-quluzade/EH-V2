package com.eHealthInsurance.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@AutoConfiguration
@ConditionalOnClass(JwtDecoder.class)
@EnableConfigurationProperties({JwtSecurityProperties.class, GatewayHeaderSecurityProperties.class})
@EnableMethodSecurity
public class EhiResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder ehiJwtDecoder(JwtSecurityProperties properties) {
        NimbusJwtDecoder decoder;
        if (StringUtils.hasText(properties.getJwkSetUri())) {
            decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        } else {
            String secret = properties.getSecret();
            Assert.hasText(secret, "ehi.security.jwt.secret or ehi.security.jwt.jwk-set-uri is required");
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
            decoder = NimbusJwtDecoder.withSecretKey(key).build();
        }
        decoder.setJwtValidator(jwtValidator(properties));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(EhiJwtAuthenticationConverter.class)
    public EhiJwtAuthenticationConverter ehiJwtAuthenticationConverter() {
        return new EhiJwtAuthenticationConverter();
    }

    @Bean
    @ConditionalOnMissingBean(ServiceTokenProvider.class)
    public ServiceTokenProvider serviceTokenProvider(JwtSecurityProperties properties) {
        Assert.hasText(properties.getSecret(), "ehi.security.jwt.secret is required for ServiceTokenProvider");
        return new ServiceTokenProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(TrustedGatewayHeaderFilter.class)
    @ConditionalOnClass(OncePerRequestFilter.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public TrustedGatewayHeaderFilter trustedGatewayHeaderFilter(GatewayHeaderSecurityProperties headerProperties,
                                                                 JwtSecurityProperties jwtProperties) {
        return new TrustedGatewayHeaderFilter(headerProperties, jwtProperties);
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(JwtSecurityProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new EhiJwtValidator(properties));
    }
}
