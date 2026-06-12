package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.dto.request.ChatRequest;
import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.dto.response.ChatResponse;
import com.ehi.ai.entity.ChatMessage;
import com.ehi.ai.mapper.ChatMessageMapper;
import com.ehi.ai.repository.ChatMessageRepository;
import com.ehi.ai.service.AiClientService;
import com.ehi.ai.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final String SYSTEM_PROMPT =
            "You are a helpful assistant for an e-health insurance company. "
                    + "Answer questions about policies, claims, and coverage concisely and politely.";

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final AiClientService aiClientService;

    @Override
    public ChatResponse sendMessage(UUID userId, ChatRequest request) {
        UUID sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID();

        List<ChatMessage> history = chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);

        List<OpenAiMessage> messages = new ArrayList<>();
        messages.add(new OpenAiMessage("system", SYSTEM_PROMPT));
        history.forEach(m -> messages.add(new OpenAiMessage(m.getRole(), m.getContent())));
        messages.add(new OpenAiMessage("user", request.message()));

        String reply = aiClientService.chatCompletion(messages);

        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role("user")
                .content(request.message())
                .build());

        chatMessageRepository.save(ChatMessage.builder()
                .userId(userId)
                .sessionId(sessionId)
                .role("assistant")
                .content(reply)
                .build());

        return new ChatResponse(sessionId, reply, Instant.now());
    }

    @Override
    public List<ChatMessageDto> getHistory(UUID userId, UUID sessionId) {
        return chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId).stream()
                .map(chatMessageMapper::toDto)
                .toList();
    }
}
