package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.dto.request.ChatRequest;
import com.ehi.ai.dto.response.ChatMessageDto;
import com.ehi.ai.dto.response.ChatResponse;
import com.ehi.ai.entity.ChatMessage;
import com.ehi.ai.mapper.ChatMessageMapper;
import com.ehi.ai.repository.ChatMessageRepository;
import com.ehi.ai.service.AiClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceImplTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock ChatMessageMapper chatMessageMapper;
    @Mock AiClientService aiClientService;

    @InjectMocks
    ChatbotServiceImpl service;

    @Test
    void sendMessage_generatesNewSessionId_whenNoneProvided() {
        UUID userId = UUID.randomUUID();
        when(chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(any(), eq(userId))).thenReturn(List.of());
        when(aiClientService.chatCompletion(any())).thenReturn("hi there");

        ChatResponse response = service.sendMessage(userId, new ChatRequest(null, "hello"));

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.reply()).isEqualTo("hi there");
    }

    @Test
    void sendMessage_reusesProvidedSessionId() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId)).thenReturn(List.of());
        when(aiClientService.chatCompletion(any())).thenReturn("hi there");

        ChatResponse response = service.sendMessage(userId, new ChatRequest(sessionId, "hello"));

        assertThat(response.sessionId()).isEqualTo(sessionId);
    }

    @Test
    void sendMessage_includesSessionHistory_andNewMessage_inAiPrompt() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        List<ChatMessage> history = List.of(
                ChatMessage.builder().userId(userId).sessionId(sessionId).role("user").content("prev q").build(),
                ChatMessage.builder().userId(userId).sessionId(sessionId).role("assistant").content("prev a").build()
        );
        when(chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId)).thenReturn(history);
        when(aiClientService.chatCompletion(any())).thenReturn("new reply");

        service.sendMessage(userId, new ChatRequest(sessionId, "new question"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OpenAiMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiClientService).chatCompletion(captor.capture());
        List<OpenAiMessage> sentMessages = captor.getValue();

        assertThat(sentMessages).hasSize(4);
        assertThat(sentMessages.get(0).role()).isEqualTo("system");
        assertThat(sentMessages.get(1)).isEqualTo(new OpenAiMessage("user", "prev q"));
        assertThat(sentMessages.get(2)).isEqualTo(new OpenAiMessage("assistant", "prev a"));
        assertThat(sentMessages.get(3)).isEqualTo(new OpenAiMessage("user", "new question"));
    }

    @Test
    void sendMessage_savesBothUserAndAssistantMessages() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId)).thenReturn(List.of());
        when(aiClientService.chatCompletion(any())).thenReturn("the reply");

        service.sendMessage(userId, new ChatRequest(sessionId, "the question"));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository, times(2)).save(captor.capture());
        List<ChatMessage> saved = captor.getAllValues();

        assertThat(saved.get(0).getRole()).isEqualTo("user");
        assertThat(saved.get(0).getContent()).isEqualTo("the question");
        assertThat(saved.get(0).getSessionId()).isEqualTo(sessionId);
        assertThat(saved.get(0).getUserId()).isEqualTo(userId);

        assertThat(saved.get(1).getRole()).isEqualTo("assistant");
        assertThat(saved.get(1).getContent()).isEqualTo("the reply");
        assertThat(saved.get(1).getSessionId()).isEqualTo(sessionId);
        assertThat(saved.get(1).getUserId()).isEqualTo(userId);
    }

    @Test
    void getHistory_returnsMappedDtos_scopedBySessionAndUser() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ChatMessage message = ChatMessage.builder().userId(userId).sessionId(sessionId).role("user").content("hi").build();
        ChatMessageDto dto = ChatMessageDto.builder().sessionId(sessionId).role("user").content("hi").build();
        when(chatMessageRepository.findBySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId)).thenReturn(List.of(message));
        when(chatMessageMapper.toDto(message)).thenReturn(dto);

        List<ChatMessageDto> result = service.getHistory(userId, sessionId);

        assertThat(result).containsExactly(dto);
    }
}
