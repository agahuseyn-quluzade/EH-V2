package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiChatRequest;
import com.ehi.ai.client.OpenAiChatResponse;
import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.config.OpenAiProperties;
import com.ehi.ai.service.AiClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiClientServiceImpl implements AiClientService {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;

    @Override
    public String chatCompletion(List<OpenAiMessage> messages) {
        OpenAiChatRequest request = new OpenAiChatRequest(openAiProperties.getModel(), messages, 0.7);

        OpenAiChatResponse response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiChatResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Empty response from OpenRouter");
        }

        return response.choices().get(0).message().content();
    }
}
