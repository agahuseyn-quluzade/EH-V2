package com.ehi.ai.controller;

import com.ehi.ai.dto.response.FraudAiResponse;
import com.ehi.ai.security.JwtProvider;
import com.ehi.ai.service.FraudDetectionService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FraudController.class)
class FraudControllerTest {

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

    @MockBean FraudDetectionService fraudDetectionService;
    @MockBean JwtProvider jwtProvider;

    private FraudAiResponse stubResponse() {
        return FraudAiResponse.builder().claimId(UUID.randomUUID()).userId(UUID.randomUUID())
                .ruleScore(40).aiScore(50).finalScore(46).flags(java.util.List.of()).build();
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "AGENT")
    void getFraudCheck_allowedForAgent() throws Exception {
        when(fraudDetectionService.getFraudCheck(any())).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/ai/fraud-checks/{claimId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getFraudCheck_allowedForAdmin() throws Exception {
        when(fraudDetectionService.getFraudCheck(any())).thenReturn(stubResponse());

        mockMvc.perform(get("/api/v1/ai/fraud-checks/{claimId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getFraudCheck_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/ai/fraud-checks/{claimId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "AGENT")
    void analyzeClaim_allowedForAgent() throws Exception {
        when(fraudDetectionService.reanalyzeClaim(any())).thenReturn(stubResponse());

        mockMvc.perform(post("/api/v1/ai/claims/{claimId}/analyze", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void analyzeClaim_forbiddenForCustomer() throws Exception {
        mockMvc.perform(post("/api/v1/ai/claims/{claimId}/analyze", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getFraudCheck_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/ai/fraud-checks/{claimId}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
}
