package com.ehi.iam.controller;

import com.ehi.iam.dto.request.ChangePasswordRequest;
import com.ehi.iam.dto.request.ChangeRoleRequest;
import com.ehi.iam.dto.request.ChangeStatusRequest;
import com.ehi.iam.dto.response.UserDto;
import com.ehi.iam.security.JwtProvider;
import com.ehi.iam.service.UserService;
import com.ehi.infra.enums.UserRole;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

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

    @MockBean UserService userService;
    // JwtAuthenticationFilter is auto-scanned as a Filter; mock JwtProvider so it can be created
    @MockBean JwtProvider jwtProvider;

    private UserDto stubDto() {
        return new UserDto(UUID.randomUUID(), "test@example.com", "John", "Doe",
                "+994501234567", UserRole.CUSTOMER, Instant.now(), true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeRole_allowedForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.changeRole(eq(id), any())).thenReturn(stubDto());

        mockMvc.perform(patch("/api/v1/users/{id}/role", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeRoleRequest(UserRole.STAFF))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void changeRole_forbiddenForCustomer() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/role", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeRoleRequest(UserRole.STAFF))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_allowedForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.changeStatus(eq(id), any())).thenReturn(stubDto());

        mockMvc.perform(patch("/api/v1/users/{id}/status", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(false))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void changeStatus_forbiddenForCustomer() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/status", UUID.randomUUID())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangeStatusRequest(false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void searchUsers_allowedForAdmin() throws Exception {
        when(userService.searchUsers(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/search").param("query", "john"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void searchUsers_allowedForAgent() throws Exception {
        when(userService.searchUsers(any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/users/search").param("query", "john"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void searchUsers_forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/v1/users/search").param("query", "john"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "CUSTOMER")
    void changePassword_allowedForAnyAuthenticatedUser() throws Exception {
        when(userService.changePassword(any(), any())).thenReturn(stubDto());

        mockMvc.perform(post("/api/v1/users/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("currentpass", "newpass123"))))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("currentpass", "newpass123"))))
                .andExpect(status().is4xxClientError());
    }
}
