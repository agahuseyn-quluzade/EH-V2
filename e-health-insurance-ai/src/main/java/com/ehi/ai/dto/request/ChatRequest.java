package com.ehi.ai.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ChatRequest(
        UUID sessionId,
        @NotBlank String message
) {
}
