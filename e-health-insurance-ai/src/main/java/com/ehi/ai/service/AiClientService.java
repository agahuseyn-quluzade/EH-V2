package com.ehi.ai.service;

import com.ehi.ai.client.OpenAiMessage;

import java.util.List;

public interface AiClientService {

    String chatCompletion(List<OpenAiMessage> messages);
}
