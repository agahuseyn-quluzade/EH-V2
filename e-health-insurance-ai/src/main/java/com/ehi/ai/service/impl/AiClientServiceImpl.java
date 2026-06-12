package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiChatRequest;
import com.ehi.ai.client.OpenAiChatResponse;
import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.config.OpenAiProperties;
import com.ehi.ai.service.AiClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientServiceImpl implements AiClientService {

    private final WebClient openAiWebClient;
    private final OpenAiProperties openAiProperties;

    @Override
    public String chatCompletion(List<OpenAiMessage> messages) {
        OpenAiChatRequest request = new OpenAiChatRequest(openAiProperties.getModel(), messages, 0.7);

        OpenAiChatResponse response;
        try {
            response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiChatResponse.class)
                    .timeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                    .block();
        } catch (Exception e) {
            log.warn("OpenRouter call failed (model={})", openAiProperties.getModel(), e);
            throw e;
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Empty response from OpenRouter");
        }

        return response.choices().get(0).message().content();
    }
}
