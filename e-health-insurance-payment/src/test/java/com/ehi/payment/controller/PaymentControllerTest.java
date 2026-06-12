package com.ehi.payment.controller;

import com.ehi.payment.dto.request.ProcessPaymentRequest;
import com.ehi.payment.dto.response.PaymentDto;
import com.ehi.payment.security.JwtProvider;
import com.ehi.payment.service.PaymentService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.PaymentReferenceType;
import com.ehi.infra.enums.PaymentStatus;
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

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

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
    @Autowired ObjectMapper objectMapper;

    @MockBean PaymentService paymentService;
    @MockBean JwtProvider jwtProvider;

    private PaymentDto stubDto() {
        return PaymentDto.builder()
                .id(UUID.randomUUID()).userId(UUID.fromString(USER_ID))
                .referenceId(UUID.randomUUID()).referenceType(PaymentReferenceType.POLICY_PREMIUM)
                .amount(BigDecimal.valueOf(100)).status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void processPayment_allowedForAdmin() throws Exception {
        when(paymentService.processPayment(any(), any(), any(), any())).thenReturn(stubDto());

        var request = new ProcessPaymentRequest(UUID.randomUUID(), UUID.randomUUID(), PaymentReferenceType.POLICY_PREMIUM, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/v1/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void processPayment_forbiddenForCustomer() throws Exception {
        var request = new ProcessPaymentRequest(UUID.randomUUID(), UUID.randomUUID(), PaymentReferenceType.POLICY_PREMIUM, BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/v1/payments/process")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getMyPayments_allowedForCustomer() throws Exception {
        when(paymentService.getMyPayments(any())).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/v1/payments/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getMyPayments_forbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/payments/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getPaymentById_allowedForAnyAuthenticatedUser() throws Exception {
        when(paymentService.getPaymentById(any(), any(), any(Boolean.class))).thenReturn(stubDto());

        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getPaymentById_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/payments/{id}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getAllPayments_allowedForAdmin() throws Exception {
        when(paymentService.getAllPayments(any())).thenReturn(
                PagedResponse.<PaymentDto>builder()
                        .content(List.of(stubDto())).page(0).size(20)
                        .totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getAllPayments_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isForbidden());
    }
}
