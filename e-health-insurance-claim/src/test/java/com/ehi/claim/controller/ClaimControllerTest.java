package com.ehi.claim.controller;

import com.ehi.claim.dto.request.ReviewClaimRequest;
import com.ehi.claim.dto.request.SubmitClaimRequest;
import com.ehi.claim.dto.response.ClaimDto;
import com.ehi.claim.security.JwtProvider;
import com.ehi.claim.service.ClaimService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.ClaimStatus;
import com.ehi.infra.enums.ClaimType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    // Minimal security config: stateless, all authenticated, method security enabled
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
    @Autowired ObjectMapper objectMapper;

    @MockBean ClaimService claimService;
    // JwtAuthenticationFilter is auto-scanned as a Filter; mock JwtProvider so it can be created
    @MockBean JwtProvider jwtProvider;

    private ClaimDto stubDto() {
        return ClaimDto.builder()
                .id(UUID.randomUUID()).claimNumber("CLM-12345678").userId(UUID.fromString(USER_ID))
                .policyId(UUID.randomUUID()).claimType(ClaimType.HOSPITALIZATION)
                .amount(BigDecimal.valueOf(500)).description("Surgery").status(ClaimStatus.SUBMITTED)
                .build();
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void submitClaim_allowedForCustomer() throws Exception {
        when(claimService.submitClaim(any(), any())).thenReturn(stubDto());

        var request = new SubmitClaimRequest(UUID.randomUUID(), ClaimType.HOSPITALIZATION, BigDecimal.valueOf(500), "Surgery");

        mockMvc.perform(post("/api/v1/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "STAFF")
    void submitClaim_forbiddenForAgent() throws Exception {
        var request = new SubmitClaimRequest(UUID.randomUUID(), ClaimType.HOSPITALIZATION, BigDecimal.valueOf(500), "Surgery");

        mockMvc.perform(post("/api/v1/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getMyClaims_allowedForCustomer() throws Exception {
        when(claimService.getMyClaims(any())).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/v1/claims/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "STAFF")
    void getAllClaims_allowedForAgent() throws Exception {
        when(claimService.getAllClaims(any(), any())).thenReturn(
                PagedResponse.<ClaimDto>builder()
                        .content(List.of(stubDto())).page(0).size(20)
                        .totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getAllClaims_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/claims"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "STAFF")
    void reviewClaim_allowedForAgent() throws Exception {
        when(claimService.reviewClaim(any(), any(), any())).thenReturn(stubDto());

        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.valueOf(400), null);

        mockMvc.perform(put("/api/v1/claims/{id}/review", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void reviewClaim_forbiddenForCustomer() throws Exception {
        var request = new ReviewClaimRequest(ClaimStatus.APPROVED, BigDecimal.valueOf(400), null);

        mockMvc.perform(put("/api/v1/claims/{id}/review", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getClaimById_allowedForAnyAuthenticatedUser() throws Exception {
        when(claimService.getClaimById(any(), any(), any(Boolean.class))).thenReturn(stubDto());

        mockMvc.perform(get("/api/v1/claims/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getClaimById_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/claims/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }
}
