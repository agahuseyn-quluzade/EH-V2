package com.eHealthInsurance.config;

import com.eHealthInsurance.common.security.EhiJwtAuthenticationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final EhiJwtAuthenticationConverter jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/initiate").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/me").hasRole("MEMBER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/**").hasAnyRole("MEMBER", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/confirm").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/succeed").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/*/fail").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/invoices/**").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/refunds").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/refunds/me").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers("/api/v1/internal/**").hasAnyRole("PAYMENT_SERVICE", "POLICY_SERVICE", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
