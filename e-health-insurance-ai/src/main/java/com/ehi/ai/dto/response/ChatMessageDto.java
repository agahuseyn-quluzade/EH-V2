package com.ehi.ai.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ChatMessageDto(
        UUID id,
        UUID sessionId,
        String role,
        String content,
        Instant createdAt
) {
}
