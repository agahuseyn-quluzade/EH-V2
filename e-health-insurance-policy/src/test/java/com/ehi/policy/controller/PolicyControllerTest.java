package com.ehi.policy.controller;

import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.PolicyStatus;
import com.ehi.policy.dto.request.CreatePlanRequest;
import com.ehi.policy.dto.request.PurchasePolicyRequest;
import com.ehi.policy.dto.response.PlanDto;
import com.ehi.policy.dto.response.PolicyDto;
import com.ehi.policy.security.JwtProvider;
import com.ehi.policy.service.PlanService;
import com.ehi.policy.service.PolicyService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PlanController.class, PolicyController.class})
class PolicyControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/plans/**").permitAll()
                            .anyRequest().authenticated());
            return http.build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean PlanService planService;
    @MockBean PolicyService policyService;
    @MockBean JwtProvider jwtProvider;

    private PlanDto stubPlanDto() {
        return PlanDto.builder()
                .id(UUID.randomUUID()).name("Basic").description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000)).premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12).active(true).build();
    }

    private PolicyDto stubPolicyDto() {
        return PolicyDto.builder()
                .id(UUID.randomUUID()).policyNumber("POL-12345678").userId(UUID.randomUUID())
                .planId(UUID.randomUUID()).planName("Basic").status(PolicyStatus.PENDING)
                .premiumAmount(BigDecimal.valueOf(50)).build();
    }

    @Test
    void getActivePlans_isPublic() throws Exception {
        when(planService.getActivePlans()).thenReturn(List.of(stubPlanDto()));

        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPlan_allowedForAdmin() throws Exception {
        when(planService.createPlan(any())).thenReturn(stubPlanDto());

        var request = CreatePlanRequest.builder()
                .name("Basic").description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000)).premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12).build();

        mockMvc.perform(post("/api/v1/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createPlan_forbiddenForCustomer() throws Exception {
        var request = CreatePlanRequest.builder()
                .name("Basic").description("Basic coverage")
                .coverageAmount(BigDecimal.valueOf(10000)).premiumAmount(BigDecimal.valueOf(50))
                .durationMonths(12).build();

        mockMvc.perform(post("/api/v1/plans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "CUSTOMER")
    void purchasePolicy_allowedForCustomer() throws Exception {
        when(policyService.purchasePolicy(any(), any())).thenReturn(stubPolicyDto());

        mockMvc.perform(post("/api/v1/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchasePolicyRequest(UUID.randomUUID()))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "ADMIN")
    void purchasePolicy_forbiddenForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PurchasePolicyRequest(UUID.randomUUID()))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "ADMIN")
    void getAllPolicies_allowedForAdmin() throws Exception {
        when(policyService.getAllPolicies(any())).thenReturn(
                PagedResponse.<PolicyDto>builder()
                        .content(List.of(stubPolicyDto())).page(0).size(20)
                        .totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = "CUSTOMER")
    void getAllPolicies_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/policies"))
                .andExpect(status().isForbidden());
    }
}
