package com.ehi.notification.controller;

import com.ehi.notification.dto.response.NotificationDto;
import com.ehi.notification.enums.NotificationStatus;
import com.ehi.notification.security.JwtProvider;
import com.ehi.notification.service.NotificationService;
import com.ehi.infra.dto.PagedResponse;
import com.ehi.infra.enums.NotificationChannel;
import com.ehi.infra.enums.NotificationType;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

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

    @MockBean NotificationService notificationService;
    @MockBean JwtProvider jwtProvider;

    private NotificationDto stubDto() {
        return NotificationDto.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .type(NotificationType.WELCOME).channel(NotificationChannel.EMAIL)
                .recipient("user@example.com").subject("Welcome").body("Hi")
                .status(NotificationStatus.SENT).retryCount(0)
                .build();
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getMyNotifications_allowedForCustomer() throws Exception {
        when(notificationService.getMyNotifications(any())).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getMyNotifications_forbiddenForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "ADMIN")
    void getAllNotifications_allowedForAdmin() throws Exception {
        when(notificationService.getAllNotifications(any())).thenReturn(
                PagedResponse.<NotificationDto>builder()
                        .content(List.of(stubDto())).page(0).size(20)
                        .totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getAllNotifications_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyNotifications_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().is4xxClientError());
    }
}
