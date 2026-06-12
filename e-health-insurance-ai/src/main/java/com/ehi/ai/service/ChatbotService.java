package com.ehi.ai.service;

import com.ehi.ai.dto.request.ChatRequest;
import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.dto.response.ChatResponse;

import java.util.List;
import java.util.UUID;

public interface ChatbotService {

    ChatResponse sendMessage(UUID userId, ChatRequest request);

    List<ChatMessageDto> getHistory(UUID userId, UUID sessionId);
}
