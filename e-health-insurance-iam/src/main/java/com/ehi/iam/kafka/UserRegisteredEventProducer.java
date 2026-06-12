package com.ehi.iam.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventProducer {

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void publish(UserRegisteredEvent event) {
        kafkaTemplate.send(KafkaTopics.USER_REGISTERED, event.userId().toString(), event);
        log.info("Published UserRegisteredEvent for userId={}", event.userId());
    }
}
