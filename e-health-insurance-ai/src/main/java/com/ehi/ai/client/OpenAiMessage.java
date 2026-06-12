package com.ehi.ai.client;

public record OpenAiMessage(
        String role,
        String content
) {
}
