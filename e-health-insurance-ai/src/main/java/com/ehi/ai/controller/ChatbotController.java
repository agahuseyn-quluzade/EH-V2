package com.ehi.ai.controller;

import com.ehi.ai.dto.request.ChatRequest;
import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.dto.response.ChatResponse;
import com.ehi.ai.service.ChatbotService;
import com.ehi.infra.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(Authentication authentication,
                                                                   @Valid @RequestBody ChatRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(chatbotService.sendMessage(userId, request)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getHistory(Authentication authentication,
                                                                          @RequestParam UUID sessionId) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(chatbotService.getHistory(userId, sessionId)));
    }
}
