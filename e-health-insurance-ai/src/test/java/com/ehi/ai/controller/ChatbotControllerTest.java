package com.ehi.ai.controller;

import com.ehi.ai.dto.request.ChatRequest;
import com.ehi.ai.dto.response.ChatResponse;
import com.ehi.ai.security.JwtProvider;
import com.ehi.ai.service.ChatbotService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
class ChatbotControllerTest {

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

    @MockBean ChatbotService chatbotService;
    @MockBean JwtProvider jwtProvider;

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void sendMessage_allowedForCustomer() throws Exception {
        when(chatbotService.sendMessage(any(), any())).thenReturn(new ChatResponse(UUID.randomUUID(), "hi", Instant.now()));

        mockMvc.perform(post("/api/v1/ai/chatbot")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest(null, "hello"))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "AGENT")
    void sendMessage_forbiddenForAgent() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chatbot")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest(null, "hello"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "CUSTOMER")
    void getHistory_allowedForCustomer() throws Exception {
        when(chatbotService.getHistory(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ai/chatbot/history").param("sessionId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = USER_ID, roles = "AGENT")
    void getHistory_forbiddenForAgent() throws Exception {
        mockMvc.perform(get("/api/v1/ai/chatbot/history").param("sessionId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessage_blocked_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chatbot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest(null, "hello"))))
                .andExpect(status().is4xxClientError());
    }
}
