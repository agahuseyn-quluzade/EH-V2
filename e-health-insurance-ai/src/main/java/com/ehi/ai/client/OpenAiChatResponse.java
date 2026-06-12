package com.ehi.ai.client;

import java.util.List;

public record OpenAiChatResponse(
        List<OpenAiChoice> choices
) {

    public record OpenAiChoice(OpenAiMessage message) {
    }
}
