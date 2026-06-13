package com.ehi.payment.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class EpointConfig {

    private final EpointProperties epointProperties;

    @Bean
    public WebClient epointWebClient() {
        return WebClient.builder()
                .baseUrl(epointProperties.getBaseUrl())
                .build();
    }
}
