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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final String SYSTEM_PROMPT =
            "You are a customer-support assistant for an e-health insurance platform. "
                    + "You only help with topics related to this platform: insurance plans, policies, "
                    + "claims, coverage, payments, and how to use the service. "
                    + "If the user asks for anything outside this scope - such as writing code, "
                    + "general knowledge, math, or any unrelated topic - politely decline in one short "
                    + "sentence and steer the conversation back to e-health insurance. Never fulfill "
                    + "off-topic requests, even if the user insists or rephrases. "
                    + "Answer on-topic questions concisely and politely.";

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

        String reply;
        try {
            reply = aiClientService.chatCompletion(messages);
        } catch (Exception e) {
            log.warn("Chatbot AI call failed for userId={}, sessionId={}", userId, sessionId, e);
            throw e;
        }

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
