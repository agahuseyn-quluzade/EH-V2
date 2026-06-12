package com.ehi.ai.service.impl;

import com.ehi.ai.client.OpenAiMessage;
import com.ehi.ai.config.OpenAiProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiClientServiceImplTest {

    private MockWebServer mockWebServer;
    private AiClientServiceImpl service;
    private OpenAiProperties openAiProperties;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        openAiProperties = new OpenAiProperties();
        openAiProperties.setModel("test-model");
        openAiProperties.setTimeoutSeconds(15);

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        service = new AiClientServiceImpl(webClient, openAiProperties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private List<OpenAiMessage> messages() {
        return List.of(new OpenAiMessage("user", "test prompt"));
    }

    @Test
    void chatCompletion_returnsContentFromFirstChoice() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hello world\"}}]}"));

        String content = service.chatCompletion(messages());

        assertThat(content).isEqualTo("hello world");
    }

    @Test
    void chatCompletion_emptyChoices_throwsIllegalStateException() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[]}"));

        assertThatThrownBy(() -> service.chatCompletion(messages()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Empty response from OpenRouter");
    }

    @Test
    void chatCompletion_slowResponse_timesOut() {
        openAiProperties.setTimeoutSeconds(1);
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"too late\"}}]}")
                .setBodyDelay(3, TimeUnit.SECONDS));

        assertThatThrownBy(() -> service.chatCompletion(messages()))
                .isInstanceOf(RuntimeException.class);
    }
}
