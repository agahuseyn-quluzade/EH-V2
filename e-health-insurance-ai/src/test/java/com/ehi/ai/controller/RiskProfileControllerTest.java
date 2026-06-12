package com.ehi.ai.controller;

import com.ehi.ai.dto.response.RiskAiResponse;
import com.ehi.ai.security.JwtProvider;
import com.ehi.ai.service.RiskProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RiskProfileController.class)
class RiskProfileControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Autowired MockMvc mockMvc;

    @MockBean RiskProfileService riskProfileService;
    @MockBean JwtProvider jwtProvider;

    private RiskAiResponse stubResponse() {
        return RiskAiResponse.builder().userId(UUID.randomUUID()).totalClaims(3)
                .averageRiskScore(20.0).highRiskCount(0).build();
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "STAFF")
    void getRiskProfile_allowedForAgent() throws Exception {
        when(riskProfileService.getRiskProfile(any())).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/ai/risk-profile/{userId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getRiskProfile_allowedForAdmin() throws Exception {
        when(riskProfileService.getRiskProfile(any())).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/ai/risk-profile/{userId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getRiskProfile_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/ai/risk-profile/{userId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRiskProfile_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/ai/risk-profile/{userId}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
}
