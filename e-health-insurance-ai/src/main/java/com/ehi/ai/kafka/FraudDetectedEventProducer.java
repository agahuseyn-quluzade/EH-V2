package com.ehi.ai.kafka;

import com.ehi.infra.config.KafkaTopics;
import com.ehi.infra.event.FraudDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudDetectedEventProducer {

    private final KafkaTemplate<String, FraudDetectedEvent> kafkaTemplate;

    public void publish(FraudDetectedEvent event) {
        kafkaTemplate.send(KafkaTopics.FRAUD_DETECTED, event.claimId().toString(), event);
        log.info("Published FraudDetectedEvent for claimId={}, riskScore={}", event.claimId(), event.riskScore());
    }
}
