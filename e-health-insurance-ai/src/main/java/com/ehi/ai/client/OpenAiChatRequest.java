package com.ehi.ai.client;

import java.util.List;

public record OpenAiChatRequest(
        String model,
        List<OpenAiMessage> messages,
        double temperature
) {
}
